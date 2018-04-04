/**
**********************************************************************************************************
--  FILENAME		: ImageDetectionController.java
--  DESCRIPTION		: Controller Class Image detection
--
--  Copyright		: Copyright (c) 2018.
--  Company			: 
--
--  Revision History
-- --------------------------------------------------------------------------------------------------------
-- |VERSION |      Date                              |      Author              |      Reason for Changes                                         |
-- --------------------------------------------------------------------------------------------------------
-- |  0.1   |   Feb 01, 2018                         |      Ankit Mohanty       |       Initial draft                                                |
-- --------------------------------------------------------------------------------------------------------
--
************************************************************************************************************
**/

package com.tensor.controllers.image;



import java.util.concurrent.ExecutionException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tensor.controllers.BaseRemoteController;
import com.tensor.exceptions.VGSException;
import com.tensor.models.ImageRecognitionTO;
import com.tensor.services.image.ImageRecognitionService;
import com.tensor.utilities.RestURLConstants;

@RestController
public class ImageDetectionController extends BaseRemoteController {
	
	@Autowired
	ImageRecognitionService imageRecognitionService;
	
	private static final Logger LOGGER = (Logger) LogManager.getLogger(ImageDetectionController.class);


	/**
	 * To upload data of Terminal entities through file upload
	 * @return JSONOutputModel
	 * @throws VGSException	
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@RequestMapping(value=RestURLConstants.IMAGE_RECOGNITION,method = RequestMethod.POST)
    @ResponseBody
    public String parseImage( @RequestParam(value="file") MultipartFile multipartFile) throws VGSException, InterruptedException, ExecutionException {
		String output =imageRecognitionService.parseImage(multipartFile);
			System.out.println(output);
			return  output;
	}
	
/*	@RequestMapping(value=RestURLConstants.IMAGE_DATA,method = RequestMethod.GET)
    @ResponseBody
    public JSONOutputModel getImageData(@PathVariable("token") String token) throws VGSException {
		JSONOutputModel output = new JSONOutputModel();
		ImageDetectionTO imageDetectionTO=imageRecognitionService.getImageData(token);
		if(imageDetectionTO!=null){
			output.setData(imageDetectionTO);
			output.setMessage("Image Detected Successfully");
		}
		else{
			output.setData(imageDetectionTO);
			output.setMessage("Image Detection Data not available");
		}
		return output;
	}*/

}
