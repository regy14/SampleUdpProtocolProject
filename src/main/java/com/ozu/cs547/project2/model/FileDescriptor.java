package com.ozu.cs547.project2.model;

public class FileDescriptor {
	private int fileId;
	private String fileName;
	
	public FileDescriptor(int fileId, String fileName) {
		this.fileId=fileId;
		this.fileName=fileName;
	}

	public int getFileId() {
		return fileId;
	}

	public void setFileId(int fileId) {
		this.fileId = fileId;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] toByte(){
		byte[] fileName=getFileName().getBytes();
		byte[] rawData=new byte[fileName.length+2];
		rawData[0]=(byte)(fileId & 0xFF);
		System.arraycopy(fileName, 0, rawData, 1, fileName.length);
		rawData[rawData.length-1]='\0';
		return rawData;
	}
	
	@Override
	public String toString() {
		return fileId+"-"+fileName;
	}
	
}
