package com.ozu.cs547.project2.model;

import com.google.common.base.MoreObjects;
import com.ozu.cs547.project2.utility.Util;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class RequestType {

    public class REQUEST_TYPES {
        public static final int GET_FILE_LIST = 1;
        public static final int GET_FILE_SIZE = 2;
        public static final int GET_FILE_DATA = 3;
    }

    //1 byte
    private int requestType;
    //1 byte
    private int fileId;
    //4 byte
    private long startByte;
    //4 byte
    private long endByte;
    byte[] data;

    public RequestType(int requestType, int fileId, long startByte, long endByte, byte[] data) {
        this.requestType = requestType;
        this.fileId = fileId;
        this.startByte = startByte;
        this.endByte = endByte;
        this.data = data;
    }

    public RequestType(byte[] rawData) {
        //request_type:1 byte|file_id:1 byte|start_byte 4 bytes|end_byte 4 bytes
        if (rawData.length < 10) {
            throw new InvalidParameterException("Invalid Header");
        }
        requestType = (int) rawData[0] & 0xFF;
        fileId = (int) rawData[1] & 0xFF;
        startByte = 0;
        for (int i = 2; i < 6; i++) {
            startByte = (startByte << 8) | ((int) rawData[i] & 0xFF);
        }
        endByte = 0;
        for (int i = 6; i < 10; i++) {
            endByte = (endByte << 8) | ((int) rawData[i] & 0xFF);
        }
        data = Arrays.copyOfRange(rawData, 10, rawData.length);
    }

    public int getRequestType() {
        return requestType;
    }

    public int getFileId() {
        return fileId;
    }

    public long getStartByte() {
        return startByte;
    }

    public long getEndByte() {
        return endByte;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("requestType", requestType)
                .add("fileId", fileId)
                .add("startByte", startByte)
                .add("endByte", endByte)
                .add("data", data)
                .toString();
    }
}
