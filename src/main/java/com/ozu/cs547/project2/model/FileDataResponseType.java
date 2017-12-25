package com.ozu.cs547.project2.model;

public class FileDataResponseType extends ResponseType {

	public FileDataResponseType(int responseType, int file_id, long start_byte, long end_byte,byte[] data) {
		super(responseType, file_id, start_byte, end_byte, data);
	}
	
	public FileDataResponseType(byte[] rawData){
		super(rawData);
	}
	
	public void setStartByte(long start_byte){
		this.startByte=start_byte;
	}
	
	public void setEndByte(long end_byte){
		this.endByte=end_byte;
	}
	
	public void setData(byte[] data){
		this.data=data;
	}
	
	@Override
	public String toString() {
		StringBuffer resultBuf=new StringBuffer("\nresponse_type:"+this.getResponseType());
		resultBuf.append("\nfile_id:"+this.getFileId());
		resultBuf.append("\nstart_byte:"+this.getStartByte());
		resultBuf.append("\nend_byte:"+this.getEndByte());
		return resultBuf.toString();
	}
}
