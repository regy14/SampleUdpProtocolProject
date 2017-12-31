package com.ozu.cs547.project2.model;

public class Stats {
    private String serverIp;
    private Integer serverPort;
    private int threadNumber;
    private long requestCounter = 0;
    private long totalRtts;
    private long lossCount = 0;
    private long downloadedByteCount = 0;
    //private String networkInterfaceName;

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public int getThreadNumber() {
        return threadNumber;
    }

    public void setThreadNumber(int threadNumber) {
        this.threadNumber = threadNumber;
    }

    public long getRequestCounter() {
        return requestCounter;
    }

    public void setRequestCounter(long requestCounter) {
        this.requestCounter = requestCounter;
    }

    public long getTotalRtts() {
        return totalRtts;
    }

    public void setTotalRtts(long totalRtts) {
        this.totalRtts = totalRtts;
    }

    public long getLossCount() {
        return lossCount;
    }

    public void setLossCount(long lossCount) {
        this.lossCount = lossCount;
    }

    public long getDownloadedByteCount() {
        return downloadedByteCount;
    }

    public void setDownloadedByteCount(long downloadedByteCount) {
        this.downloadedByteCount = downloadedByteCount;
    }
}
