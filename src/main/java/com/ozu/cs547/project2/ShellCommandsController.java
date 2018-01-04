package com.ozu.cs547.project2;

import com.ozu.cs547.project2.model.FileDescriptor;
import com.ozu.cs547.project2.model.InetAddressInterface;
import com.sun.security.ntlm.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

@ShellComponent
public class ShellCommandsController {

    @Autowired
    ServerCommunicator serverCommunicator;

    @ShellMethod("FTP client creater")
    private void createclient(@ShellOption String firstIpPort, String secondIpPort) {
        Scanner scanner = new Scanner(System.in);
        String tokens[] = firstIpPort.split(":");
        String server1Ip = tokens[0];
        Integer server1Port = Integer.parseInt(tokens[1]);
        tokens = secondIpPort.split(":");
        String server2Ip = tokens[0];
        Integer server2Port = Integer.parseInt(tokens[1]);
        try {
            List<FileDescriptor> fileList = serverCommunicator.getFileList(server1Ip, server1Port);
            System.out.println("Please select a file for download(Enter the file id) : ");
            fileList.stream().forEach(System.out::println);
            int fileId = scanner.nextInt();
            System.out.format("File %d has been selected. Getting the size information… \n", fileId);
            Optional<FileDescriptor> file = fileList.stream().filter(thisFile -> thisFile.getFileId() == fileId).findFirst();
            long fileSize = serverCommunicator.getFileSize(server1Ip, server1Port, fileId);
            System.out.format("File %d is %d bytes. Starting to download…\n", fileId, fileSize);

            serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 8);
            serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 8);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
