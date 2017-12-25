package com.ozu.cs547.project2.model;

import com.google.common.base.MoreObjects;

import java.security.InvalidParameterException;
import java.util.Arrays;

public class ResponseType {
	
	public static final int HEADER_SIZE=10;
	public static final int MAX_DATA_SIZE=1000;
	public static final int MAX_RESPONSE_SIZE=HEADER_SIZE+MAX_DATA_SIZE;
	
	public class RESPONSE_TYPES{
		public static final int GET_FILE_LIST_SUCCESS=1;
		public static final int GET_FILE_SIZE_SUCCESS=2;
		public static final int GET_FILE_DATA_SUCCESS=3;
		
		public static final int INVALID_REQUEST_TYPE=100;
		public static final int INVALID_FILE_ID=101;
		public static final int INVALID_START_OR_END_BYTE=102;
	}
	
	//1 byte
	private int responseType;
	//1 byte
	private int fileId;
	//4 byte
	protected long startByte;
	//4 byte
	protected long endByte;
	protected byte[] data;
	
	public ResponseType(int responseType, int fileId, long startByte, long endByte,byte[] data){
		this.responseType=responseType;
		this.fileId=fileId;
		this.startByte=startByte;
		this.endByte=endByte;
		this.data=data;
	}
	
	public ResponseType(byte[] rawData) {
		//request_type:1 byte|file_id:1 byte|start_byte 4 bytes|end_byte 4 bytes
		if (rawData.length<10){
			throw new InvalidParameterException("Invalid Header");
		}
		responseType=(int)rawData[0] & 0xFF;
		fileId=(int)rawData[1] & 0xFF;
		startByte=0;
		for(int i=2;i<6;i++){
			startByte=(startByte << 8)|((int)rawData[i] & 0xFF);
		}
		endByte=0;
		for(int i=6;i<10;i++){
			endByte=(endByte << 8)|((int)rawData[i] & 0xFF);
		}
		int dataLength=(int)(endByte-startByte+1);
		if (responseType== RESPONSE_TYPES.GET_FILE_LIST_SUCCESS){
			data=Arrays.copyOfRange(rawData, 10, rawData.length);
		}
		else if (responseType== RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			data=Arrays.copyOfRange(rawData, 10, 14);
		}
		else if (responseType== RESPONSE_TYPES.GET_FILE_DATA_SUCCESS){
			if ((dataLength+10)>rawData.length){
				throw new InvalidParameterException("Data length does not match with the header");
			}
			data=Arrays.copyOfRange(rawData, 10, 10+dataLength);
		}
	}

	public int getResponseType() {
		return responseType;
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
				.add("responseType", responseType)
				.add("fileId", fileId)
				.add("startByte", startByte)
				.add("endByte", endByte)
				.add("data", data)
				.toString();
	}
}
