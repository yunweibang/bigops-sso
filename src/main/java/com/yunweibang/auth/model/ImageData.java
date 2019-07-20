package com.yunweibang.auth.model;

public class ImageData {
	private String contentType;
	private byte[] data;

	public ImageData() {
		super();
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}
