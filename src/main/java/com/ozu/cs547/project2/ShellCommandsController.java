package com.ozu.cs547.project2;

import com.ozu.cs547.project2.model.FileDescriptor;
import com.ozu.cs547.project2.model.InetAddressInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
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
            List<FileDescriptor> fileList = serverCommunicator.getFileList(server1Ip, server1Port, null);

            //check which interface can access server
            List<InetAddressInterface> accessableInterfaces = getAvailableInterfaceList(server1Ip, server1Port);

            System.out.println("Please select a file for download(Enter the file id) : ");
            fileList.stream().forEach(System.out::println);
            int fileId = scanner.nextInt();
            System.out.format("File %d has been selected. Getting the size information… \n", fileId);
            Optional<FileDescriptor> file = fileList.stream().filter(thisFile -> thisFile.getFileId() == fileId).findFirst();
            long fileSize = serverCommunicator.getFileSize(server1Ip, server1Port, fileId);
            System.out.format("File %d is %d bytes. Starting to download…\n", fileId, fileSize);
            accessableInterfaces.stream().forEach(inetAddr -> {
                serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 4 * accessableInterfaces.size(), inetAddr);
                serverCommunicator.readFileFromServer(server1Ip, server1Port, fileId, file.get().getFileName(), fileSize, 4 * accessableInterfaces.size(), inetAddr);
                serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 4 * accessableInterfaces.size(), inetAddr);
                serverCommunicator.readFileFromServer(server2Ip, server2Port, fileId, file.get().getFileName(), fileSize, 4 * accessableInterfaces.size(), inetAddr);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<InetAddressInterface> getAvailableInterfaceList(String server1Ip, Integer server1Port) {
        List<InetAddressInterface> addresses = new ArrayList<InetAddressInterface>();
        List<InetAddressInterface> availableAddresses = new ArrayList<>();
        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface net : Collections.list(nets)) {
                if (net.isUp() && !net.isLoopback() && !net.isVirtual() && !net.isPointToPoint()) {
                    for (InetAddress inet_adr : Collections.list(net.getInetAddresses())) {
                        if (inet_adr instanceof Inet4Address) {
                            InetAddressInterface adr = new InetAddressInterface(net, inet_adr);
                            addresses.add(adr);
                        }
                    }
                }
            }
            boolean successFullyTried = false;
            for (InetAddressInterface inetAddr : addresses) {
                while (!successFullyTried) {
                    try {
                        serverCommunicator.getFileList(server1Ip, server1Port, inetAddr);
                        availableAddresses.add(inetAddr);
                        successFullyTried = true;
                    } catch (IOException e) {
                        if (e.getMessage() != null && e.getMessage().equals("Network is unreachable: Datagram send failed")) {
                            successFullyTried = true;
                        }
                    }
                }
                successFullyTried = false;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return availableAddresses;
    }


}
