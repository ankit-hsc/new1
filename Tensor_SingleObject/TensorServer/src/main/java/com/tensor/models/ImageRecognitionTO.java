package com.tensor.models;

import org.springframework.web.multipart.MultipartFile;

public class ImageRecognitionTO {
	
	
	private MultipartFile file;
	private String fileName;
	private String filePath;

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	

	
	
}

