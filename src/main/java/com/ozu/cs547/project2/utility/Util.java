package com.ozu.cs547.project2.utility;


public class Util {
    public static byte[] toByteArray(byte[] data, int requestType, int fileId, long startByte, long endByte) {
        int dataLength = 0;
        if (data != null) {
            dataLength = data.length;
        }
        byte[] rawData = new byte[10 + dataLength];
        rawData[0] = (byte) (requestType & 0xFF);
        rawData[1] = (byte) (fileId & 0xFF);
        long tmp = startByte;
        for (int i = 5; i > 1; i--) {
            rawData[i] = (byte) (tmp & 0xFF);
            tmp >>= 8;
        }
        tmp = endByte;
        for (int i = 9; i > 5; i--) {
            rawData[i] = (byte) (tmp & 0xFF);
            tmp >>= 8;
        }
        if (data != null) {
            System.arraycopy(data, 0, rawData, 10, dataLength);
        }
        return rawData;
    }
}
