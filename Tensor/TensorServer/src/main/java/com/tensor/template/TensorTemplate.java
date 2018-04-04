package com.tensor.template;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

public class TensorTemplate {

	
	
	public   String singleObjectTensor(File  file) throws JSONException   {

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		FileSystemResource value = new FileSystemResource(file);
		map.add("file", value);

		String postUrl = "http://10.11.26.13:8080/tensor/imageRecgnition";

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, httpHeaders);


		//JSONObject json = new JSONObject();
		//json.put("fileName",name);
		//json.put("filePath", path);
		//json.put("file", multipartFile);
		//String response = restTemplate.postForObject(postUrl, requestEntity, String.class);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response  = restTemplate.exchange(postUrl, HttpMethod.POST, requestEntity, String.class);
		return response.getBody();
	}
	
	
	
	public  String multipleObjectTensor(File  file) throws JSONException   {

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		FileSystemResource value = new FileSystemResource(file);
		map.add("file", value);
		
		String postUrl = "http://10.11.26.55:8080/tensor/imageRecgnition";

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, httpHeaders);


		/*JSONObject json = new JSONObject();
		json.put("fileName",name);
		json.put("filePath", path);
		HttpEntity <String> httpEntity = new HttpEntity <String> (json.toString(), httpHeaders);*/

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response  = restTemplate.exchange(postUrl, HttpMethod.POST, requestEntity, String.class);
		return response.getBody();
	}
}
