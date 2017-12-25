package com.ozu.cs547.project2;

import com.ozu.cs547.project2.model.*;
import com.ozu.cs547.project2.utility.Util;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ServerCommunicator {

    private AtomicInteger PACKET_COUNTER = new AtomicInteger(0);
    private Boolean WRITE_FLAG_ACTIVE = false;
    private ConcurrentMap<Integer, List<byte[]>> fileContent = new ConcurrentHashMap<>();

    @Async
    public void readFileFromServer(String serverIp, int serverPort, int fileId, String fileName, long fileSize) {
        try {
            int counterVal = PACKET_COUNTER.getAndIncrement();
            while (counterVal * 1000 < fileSize) {
                List<byte[]> content;
                if ((counterVal + 1) * 1000 >= fileSize) {
                    content = getFileData(serverIp, serverPort, fileId, (counterVal * 1000) + 1, fileSize);
                } else {
                    content = getFileData(serverIp, serverPort, fileId, (counterVal * 1000) + 1, (counterVal + 1) * 1000);
                }
                fileContent.put(counterVal, content);
                counterVal = PACKET_COUNTER.getAndIncrement();
            }
            synchronized (WRITE_FLAG_ACTIVE) {
                if (!WRITE_FLAG_ACTIVE) {
                    WRITE_FLAG_ACTIVE = true;
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
                } else {
                    //diğer thread yazma işini yapmış demektir yeni file için enable et
                    WRITE_FLAG_ACTIVE = false;
                }
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
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        dsocket.receive(receivePacket);
        FileListResponseType response = new FileListResponseType(receivePacket.getData());
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
        dsocket.receive(receivePacket);
        FileSizeResponseType response = new FileSizeResponseType(receivePacket.getData());
        return response.getFileSize();
    }

    private List<byte[]> getFileData(String ip, int port, int file_id, long start, long end) throws IOException {
        List<byte[]> returnList = new ArrayList<>();
        InetAddress IPAddress = InetAddress.getByName(ip);
        RequestType req = new RequestType(RequestType.REQUEST_TYPES.GET_FILE_DATA, file_id, start, end, null);
        byte[] sendData = Util.toByteArray(req.getData(), req.getRequestType(), req.getFileId(), req.getStartByte(), req.getEndByte());
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(sendPacket);
        byte[] receiveData = new byte[ResponseType.MAX_RESPONSE_SIZE];
        long maxReceivedByte = -1;
        while (maxReceivedByte < end) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            dsocket.receive(receivePacket);
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
}
