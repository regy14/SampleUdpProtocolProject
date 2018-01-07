package com.ozu.cs547.project2;

import com.ozu.cs547.project2.model.*;
import com.ozu.cs547.project2.utility.Util;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ServerCommunicator {

    private Date startTime = null;
    private AtomicLong PACKET_COUNTER = new AtomicLong(0);
    private AtomicInteger THREAD_ENUMARATOR = new AtomicInteger(1);
    private AtomicInteger FINISHED_THREAD_COUNTER = new AtomicInteger(0);
    private ConcurrentMap<Long, List<byte[]>> fileContent = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<Long> needHelpBytes = new ConcurrentLinkedQueue<>();
    private List<Stats> threadStats = new ArrayList<>();
    private Long currentDownloadingFileSize = null;

    @Async
    public void readFileFromServer(String serverIp, int serverPort, int fileId, String fileName, long fileSize, int numberOfThreads, InetAddressInterface inetAddr) {
        try {
            int readByteSize = 10000;
            if (fileSize > 10000000) {
                readByteSize = 50000;
            }
            Long startTimeInMilis, endTimeInMilis;
            Stats currentThreadStats = new Stats();
            currentThreadStats.setServerIp(serverIp);
            currentThreadStats.setServerPort(serverPort);
            currentThreadStats.setThreadNumber(THREAD_ENUMARATOR.getAndIncrement());
            if (currentDownloadingFileSize == null) {
                synchronized (this) {
                    currentDownloadingFileSize = fileSize;
                }
            }

            threadStats.add(currentThreadStats);
            DatagramSocket dsocket;
            if (inetAddr != null) {
                dsocket = new DatagramSocket(new InetSocketAddress(inetAddr.getNetworkInterface().getInetAddresses().nextElement(), 0));
            } else {
                dsocket = new DatagramSocket();
            }
            dsocket.setSoTimeout(1000);//default timeout value 1000 ms after first calculation it will be average rtt*3
            long counterVal = PACKET_COUNTER.getAndIncrement();
            if (startTime == null || (startTime.getTime() + 2000) < (new Date()).getTime()) {
                startTime = new Date();
            }
            boolean isDownloadCompleted;
            int retryCounter = 0;
            while (counterVal * readByteSize < fileSize) {
                List<byte[]> content = null;
                retryCounter = 0;
                if ((counterVal + 1) * readByteSize >= fileSize) {
                    isDownloadCompleted = false;
                    while (!isDownloadCompleted) {
                        retryCounter++;
                        startTimeInMilis = System.currentTimeMillis();
                        content = getFileData(serverIp, serverPort, fileId, (counterVal * readByteSize) + 1, fileSize,
                                dsocket, currentThreadStats);
                        endTimeInMilis = System.currentTimeMillis();
                        if (content.stream().map(x -> x.length).mapToInt(i -> i).sum() == (fileSize - (counterVal * readByteSize))) {
                            isDownloadCompleted = true;
                            currentThreadStats.setDownloadedByteCount(currentThreadStats.getDownloadedByteCount() +
                                    (fileSize - (counterVal * readByteSize)));
                            fileContent.put(counterVal, content);
                        } else {
                            if (needHelpBytes.contains(counterVal) == false) {
                                if (fileContent.get(counterVal) != null) {
                                    isDownloadCompleted = true; //Another thread downloaded bytes
                                } else {
                                    needHelpBytes.add(counterVal);
                                }
                            }
                            currentThreadStats.setLossCount(currentThreadStats.getLossCount() + 1);
                        }
                        currentThreadStats.setRequestCounter(currentThreadStats.getRequestCounter() + 1);
                        currentThreadStats.setTotalRtts(currentThreadStats.getTotalRtts() + (endTimeInMilis - startTimeInMilis));
                        dsocket.setSoTimeout(calculateTimeout(currentThreadStats, retryCounter));
                    }
                } else {
                    isDownloadCompleted = false;
                    while (!isDownloadCompleted) {
                        retryCounter++;
                        startTimeInMilis = System.currentTimeMillis();
                        content = getFileData(serverIp, serverPort, fileId, (counterVal * readByteSize) + 1,
                                (counterVal + 1) * readByteSize, dsocket, currentThreadStats);
                        endTimeInMilis = System.currentTimeMillis();
                        if (content.stream().map(x -> x.length).mapToInt(i -> i).sum() == readByteSize) {
                            isDownloadCompleted = true;
                            currentThreadStats.setDownloadedByteCount(currentThreadStats.getDownloadedByteCount() + readByteSize);
                            fileContent.put(counterVal, content);
                        } else {
                            if (needHelpBytes.contains(counterVal) == false) {
                                if (fileContent.get(counterVal) != null) {
                                    isDownloadCompleted = true; //Another thread downloaded bytes
                                } else {
                                    needHelpBytes.add(counterVal);
                                }
                            }
                            currentThreadStats.setLossCount(currentThreadStats.getLossCount() + 1);
                        }
                        currentThreadStats.setRequestCounter(currentThreadStats.getRequestCounter() + 1);
                        currentThreadStats.setTotalRtts(currentThreadStats.getTotalRtts() + (endTimeInMilis - startTimeInMilis));
                        dsocket.setSoTimeout(calculateTimeout(currentThreadStats, retryCounter));
                    }
                }
                if (!needHelpBytes.isEmpty()) {
                    try {
                        counterVal = needHelpBytes.poll();
                    } catch (Exception e) {
                        //another thread removed from queue
                        counterVal = PACKET_COUNTER.getAndIncrement();
                    }
                } else {
                    counterVal = PACKET_COUNTER.getAndIncrement();
                }
            }
            if (FINISHED_THREAD_COUNTER.incrementAndGet() == numberOfThreads) {
                //write file content
                FileOutputStream fos = new FileOutputStream(fileName);
                fileContent.keySet().stream().sorted().forEach(key -> {
                    fileContent.get(key).stream().forEach(data -> {
                        try {
                            fos.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                });
                fos.close();

                FINISHED_THREAD_COUNTER.set(0);
                PACKET_COUNTER.set(0);
                fileContent.clear();
                threadStats.clear();
                currentDownloadingFileSize = null;
                long totalTimeInMilis = (new Date()).getTime() - startTime.getTime();
                System.out.println("File " + fileId + " has been downloaded.\n Total download time : " + totalTimeInMilis +
                        " msecs. The md5 hash is " + getFileMD5Checksum(fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int calculateTimeout(Stats currentThreadStats, int retryCounter) {
        int timeout = (int) ((currentThreadStats.getTotalRtts() / currentThreadStats.getRequestCounter()) * (Math.pow(2, retryCounter)));
        if (timeout > 3000) {
            timeout = 3000;
        }
        return timeout;
    }

    public List<FileDescriptor> getFileList(String ip, int port, InetAddressInterface inetAddr) throws IOException {
        List<String> fileNames = new ArrayList<>();
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        byte[] sendData = Util.toByteArray(req.getData(), req.getRequestType(), req.getFileId(), req.getStartByte(), req.getEndByte());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        DatagramSocket dsocket;
        if (inetAddr != null) {
            dsocket = new DatagramSocket(new InetSocketAddress(inetAddr.getNetworkInterface().getInetAddresses().nextElement(), 0));
        } else {
            dsocket = new DatagramSocket();
        }
        dsocket.setSoTimeout(1000);
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        getDatagramPacketResponse(sendPacket, dsocket, receivePacket);
        FileListResponseType response = new FileListResponseType(receivePacket.getData());
        dsocket.close();
        return Arrays.asList(response.getFileDescriptors());
    }

    public long getFileSize(String ip, int port, int file_id) throws IOException {
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_SIZE, file_id, 0, 0, null);
        byte[] sendData = Util.toByteArray(req.getData(), req.getRequestType(), req.getFileId(), req.getStartByte(), req.getEndByte());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.setSoTimeout(1000);
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        getDatagramPacketResponse(sendPacket, dsocket, receivePacket);
        FileSizeResponseType response = new FileSizeResponseType(receivePacket.getData());
        dsocket.close();
        return response.getFileSize();
    }

    private void getDatagramPacketResponse(DatagramPacket sendPacket, DatagramSocket dsocket, DatagramPacket receivePacket) throws IOException {
        boolean isPacketReceived = false;
        while (!isPacketReceived) {
            try {
                dsocket.receive(receivePacket);
                isPacketReceived = true;
            } catch (Exception ex) {
                dsocket.close();
                dsocket = new DatagramSocket();
                dsocket.setSoTimeout(1000);
                dsocket.send(sendPacket);
            }
        }
    }

    private List<byte[]> getFileData(String ip, int port, int file_id, long start, long end, DatagramSocket dsocket, Stats currentThreadStats) throws IOException {
        List<byte[]> returnList = new ArrayList<>();
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = Util.toByteArray(req.getData(), req.getRequestType(), req.getFileId(), req.getStartByte(), req.getEndByte());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte = -1;
        while (maxReceivedByte < end) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                dsocket.receive(receivePacket);
            } catch (Exception e) {
                //timeout or packet loss occured break loop
                returnList.clear();
                break;
            }
            FileDataResponseType response = new FileDataResponseType(receivePacket.getData());
            if (response.getResponseType() != ResponseType.RESPONSE_TYPES.GET_FILE_DATA_SUCCESS) {
                returnList.clear();
                break;
            }
            if (response.getEndByte() > maxReceivedByte) {
                returnList.add(response.getData());
                maxReceivedByte = response.getEndByte();
            }
        }
        return returnList;
    }

    @Scheduled(fixedRate = 3000)
    public void statsPrinter() {
        if (threadStats != null && !threadStats.isEmpty()) {
            Map<String, Integer> serversDownloadedBytes = new HashMap<>();
            Map<String, Float> serversTotalRtts = new HashMap<>();
            Map<String, Long> serversPacketLossCount = new HashMap<>();
            Map<String, Long> serversRequestCount = new HashMap<>();
            Long totalElapsedTimeAsMs = Calendar.getInstance().getTime().getTime() - startTime.getTime();
            threadStats.stream().forEach(stat -> {
                //total downloaded byte count for each server
                String key = stat.getServerIp() + ":" + stat.getServerPort();
                int downloadedByteCounter = serversDownloadedBytes.get(key) == null ? 0 : serversDownloadedBytes.get(key);
                downloadedByteCounter += stat.getDownloadedByteCount();
                serversDownloadedBytes.put(key, downloadedByteCounter);
                //Averge RTT calculation for each server
                float totalRtt = serversTotalRtts.get(key) == null ? 0 : serversTotalRtts.get(key);
                try {
                    totalRtt += stat.getTotalRtts();
                } catch (ArithmeticException e) {
                    //recently started divison by zero exception
                }
                serversTotalRtts.put(key, totalRtt);

                //Package Loss Counts
                Long packageLossCount = serversPacketLossCount.get(key) == null ? 0L : serversPacketLossCount.get(key);
                try {
                    packageLossCount += stat.getLossCount();
                } catch (ArithmeticException e) {
                    //recently started divison by zero exception
                }
                serversPacketLossCount.put(key, packageLossCount);

                //Servers total request counts
                Long requestCount = serversRequestCount.get(key) == null ? 0L : serversRequestCount.get(key);
                requestCount += stat.getRequestCounter();
                serversRequestCount.put(key, requestCount);
            });
            float percentage = (float) serversDownloadedBytes.values().stream().mapToLong(l -> l).sum() / currentDownloadingFileSize;
            System.out.printf("Currently elapsed time : %d ms. Total download percentage : %.2f \n", totalElapsedTimeAsMs, percentage);
            serversDownloadedBytes.keySet().stream().forEach(key -> {
                System.out.println("=============================== Server " + key + " Current Stats : ===============================");
                System.out.printf("Total Downloaded Bytes : %d bytes \n", serversDownloadedBytes.get(key));
                System.out.printf("Transfer speed : %.2f  byte/sec \n", ((float) serversDownloadedBytes.get(key) / ((float) totalElapsedTimeAsMs / 1000)));
                System.out.printf("Average Rtt : %.2f ms\n", ((float) serversTotalRtts.get(key) / serversRequestCount.get(key)));
                System.out.printf("Package Loss Rate : %.2f \n", ((float) serversPacketLossCount.get(key) / serversRequestCount.get(key)));
                System.out.println("###################################################################################################\n\n");
            });
        }
    }

    private String getFileMD5Checksum(String filename) {
        String returnVal = "";
        try (InputStream is = Files.newInputStream(Paths.get(filename))) {
            returnVal = DigestUtils.md5DigestAsHex(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returnVal;
    }
}
