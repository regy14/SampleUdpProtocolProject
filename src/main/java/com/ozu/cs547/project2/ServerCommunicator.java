package com.ozu.cs547.project2;

import com.ozu.cs547.project2.model.*;
import com.ozu.cs547.project2.utility.Util;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ServerCommunicator {

    private Date startTime = null;
    private AtomicInteger PACKET_COUNTER = new AtomicInteger(0);
    private AtomicInteger THREAD_ENUMARATOR = new AtomicInteger(1);
    private AtomicInteger FINISHED_THREAD_COUNTER = new AtomicInteger(0);
    private ConcurrentMap<Integer, List<byte[]>> fileContent = new ConcurrentHashMap<>();
    private List<Stats> threadStats = new ArrayList<>();

    @Async
    public void readFileFromServer(String serverIp, int serverPort, int fileId, String fileName, long fileSize, int numberOfThreads) {
        try {
            Long startTimeInMilis, endTimeInMilis;
            Stats currentThreadStats = new Stats();
            currentThreadStats.setServerIp(serverIp);
            currentThreadStats.setServerPort(serverPort);
            currentThreadStats.setThreadNumber(THREAD_ENUMARATOR.getAndIncrement());
            threadStats.add(currentThreadStats);
            DatagramSocket dsocket = new DatagramSocket();
            dsocket.setSoTimeout(1000);//default timeout value 1000 ms after first calculation it will be average rtt*3
            int counterVal = PACKET_COUNTER.getAndIncrement();
            if (startTime == null) {
                startTime = new Date();
            }
            boolean isDownloadCompleted = false;
            while (counterVal * 1000 < fileSize) {
                List<byte[]> content = null;
                if ((counterVal + 1) * 1000 >= fileSize) {
                    isDownloadCompleted = false;
                    while (!isDownloadCompleted) {
                        startTimeInMilis = System.currentTimeMillis();
                        content = getFileData(serverIp, serverPort, fileId, (counterVal * 1000) + 1, fileSize,
                                dsocket, currentThreadStats);
                        endTimeInMilis = System.currentTimeMillis();
                        if (content.stream().map(x -> x.length).mapToInt(i -> i).sum() == (fileSize - (counterVal * 1000))) {
                            isDownloadCompleted = true;
                            currentThreadStats.setDownloadedByteCount(currentThreadStats.getDownloadedByteCount() +
                                    (fileSize - (counterVal * 1000)));
                        } else {
                            currentThreadStats.setLossCount(currentThreadStats.getLossCount() + 1);
                        }
                        currentThreadStats.setRequestCounter(currentThreadStats.getRequestCounter() + 1);
                        currentThreadStats.setTotalRtts(currentThreadStats.getTotalRtts() + (endTimeInMilis - startTimeInMilis));
                        dsocket.setSoTimeout((int) (currentThreadStats.getTotalRtts() / currentThreadStats.getRequestCounter()) * 3);
                    }
                } else {
                    isDownloadCompleted = false;
                    while (!isDownloadCompleted) {
                        startTimeInMilis = System.currentTimeMillis();
                        content = getFileData(serverIp, serverPort, fileId, (counterVal * 1000) + 1,
                                (counterVal + 1) * 1000, dsocket, currentThreadStats);
                        endTimeInMilis = System.currentTimeMillis();
                        if (content.stream().map(x -> x.length).mapToInt(i -> i).sum() == 1000) {
                            isDownloadCompleted = true;
                            currentThreadStats.setDownloadedByteCount(currentThreadStats.getDownloadedByteCount() + 1000);
                        } else {
                            currentThreadStats.setLossCount(currentThreadStats.getLossCount() + 1);
                        }
                        currentThreadStats.setRequestCounter(currentThreadStats.getRequestCounter() + 1);
                        currentThreadStats.setTotalRtts(currentThreadStats.getTotalRtts() + (endTimeInMilis - startTimeInMilis));
                        dsocket.setSoTimeout((int) (currentThreadStats.getTotalRtts() / currentThreadStats.getRequestCounter()) * 3);
                    }
                }
                fileContent.put(counterVal, content);
                counterVal = PACKET_COUNTER.getAndIncrement();
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
                System.out.println("File " + fileId + " has been downloaded. The md5 hash is " + getFileMD5Checksum(fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<FileDescriptor> getFileList(String ip, int port) throws IOException {
        List<String> fileNames = new ArrayList<>();
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_LIST, 0, 0, 0, null);
        byte[] sendData = Util.toByteArray(req.getData(), req.getRequestType(), req.getFileId(), req.getStartByte(), req.getEndByte());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
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
                break;
            }
            FileDataResponseType response = new FileDataResponseType(receivePacket.getData());
            if (response.getResponseType() != ResponseType.RESPONSE_TYPES.GET_FILE_DATA_SUCCESS) {
                break;
            }
            if (response.getEndByte() > maxReceivedByte) {
                returnList.add(response.getData());
                maxReceivedByte = response.getEndByte();
            }
        }
        return returnList;
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
