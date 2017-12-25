package com.ozu.cs547.project2.model;

import com.google.common.base.MoreObjects;

public class FileSizeResponseType extends ResponseType {
	
	long fileSize=-1;
	
	public FileSizeResponseType(int responseType, int fileId, long startByte, long endByte,long fileSize) {
		super(responseType, fileId, startByte, endByte, null);
		this.fileSize=fileSize;
		setFileSizeToData();
	}
	
	public FileSizeResponseType(byte[] rawData){
		super(rawData);
		setFileSize();
	}
	
	private void setFileSize(){
		if (this.getResponseType()==ResponseType.RESPONSE_TYPES.GET_FILE_SIZE_SUCCESS){
			byte[] data=this.getData();
			fileSize=0;
			for(int i=0;i<4;i++){
				fileSize=(fileSize << 8)|((int)data[i] & 0xFF);
			}
		}
	}
	
	private void setFileSizeToData(){
		this.data=new byte[4];
		long tmp=fileSize;
		for(int i=3;i>=0;i--){
			this.data[i]=(byte)(tmp & 0xFF);
			tmp>>=8;
		}
	}
	
	public long getFileSize(){
		return fileSize;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fileSize", fileSize)
				.add("startByte", startByte)
				.add("endByte", endByte)
				.add("data", data)
				.toString();
	}
}
