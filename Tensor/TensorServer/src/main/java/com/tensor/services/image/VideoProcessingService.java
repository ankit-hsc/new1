package com.tensor.services.image;

import static org.jcodec.codecs.h264.H264Utils.splitMOVPacket;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420jToRgb;
import org.json.JSONException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.tensor.ebml.InputStreamParserByteSource;
import com.tensor.ebml.MkvTypeInfos;
import com.tensor.examples.KinesisVideoExample;
import com.tensor.mkv.Frame;
import com.tensor.mkv.MkvDataElement;
import com.tensor.mkv.MkvElement;
import com.tensor.mkv.MkvElementVisitException;
import com.tensor.mkv.StreamingMkvReader;
import com.tensor.template.TensorTemplate;
import com.tensor.utilities.ApplicationConstants;
import com.tensor.utilities.FragmentMetadataVisitor;
import com.tensor.utilities.MkvTrackMetadata;

@Transactional
@Repository

public class VideoProcessingService {
	private static final H264Decoder decoder = new H264Decoder();
	private static final Transform transform = new Yuv420jToRgb();
	private static byte[] codecPrivateData;
	public  String videoToken;
	public  String tensorName;
	public int frameCounter=0;
	public int singleCounter=1;
	public int doubleCounter=1;

	private static final Logger LOGGER = (Logger) LogManager.getLogger(VideoProcessingService.class.getName());	
	
	@PersistenceContext	
	private EntityManager entityManager;
	
	
	
	
	public void streamVideo(String token,InputStream inputStream) throws InterruptedException,IOException, MkvElementVisitException, ExecutionException, JSONException{
		this.videoToken=token;
		KinesisVideoExample example = KinesisVideoExample.builder().region(Regions.US_EAST_1)
				.streamName("HSC_"+token)
				.credentialsProvider(new SystemPropertiesCredentialsProvider())
				.inputVideoStream(inputStream)
				.build();

		example.execute(token);
		createFrame(token);
		//example.getFragmentsPersisted();
	    //example.getFragmentsRead();
	}
	
	
	public void createFrame(String token) throws IOException, MkvElementVisitException, InterruptedException, ExecutionException, JSONException {
		
		InputStream in= new FileInputStream(ApplicationConstants.RESOURCE_PATH+File.separator+"merged_output_"+token+".mkv");;
		FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create();
		StreamingMkvReader mkvStreamReader =StreamingMkvReader.createDefault(new InputStreamParserByteSource(in));
		
		while(mkvStreamReader.mightHaveNext()) {
				File frameDir =new File(ApplicationConstants.FRAME_PATH+File.separator+token);
					Optional<MkvElement> mkvElement = mkvStreamReader.nextIfAvailable();
					if (mkvElement.isPresent()) {
						mkvElement.get().accept(fragmentVisitor);
						if (MkvTypeInfos.SIMPLEBLOCK.equals(mkvElement.get().getElementMetaData().getTypeInfo())) {
							MkvDataElement dataElement = (MkvDataElement) mkvElement.get();
							Frame frame = (Frame) dataElement.getValueCopy().getVal();
							MkvTrackMetadata trackMetadata = fragmentVisitor.getMkvTrackMetadata(frame.getTrackNumber());
							process(frame,trackMetadata,frameDir);
						}
					}
				}
        	}
	
	public  void process(Frame frame, MkvTrackMetadata trackMetadata,File frameDir) throws IOException {
		
		ByteBuffer frameBuffer = frame.getFrameData();
		byte[] frameArray=frameBuffer.array();
		int pixelWidth = trackMetadata.getPixelWidth().get().intValue();
		int pixelHeight = trackMetadata.getPixelHeight().get().intValue();
		byte[] codecPrivateData = trackMetadata.getCodecPrivateData().array();
		Picture rgb = Picture.create(pixelWidth, pixelHeight, ColorSpace.RGB);
		BufferedImage renderImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_3BYTE_BGR);
		AvcCBox avcC = AvcCBox.parseAvcCBox(ByteBuffer.wrap(codecPrivateData));
		decoder.addSps(avcC.getSpsList());
		decoder.addPps(avcC.getPpsList());
		Picture buf = Picture.create(pixelWidth, pixelHeight, ColorSpace.YUV420J);

		List<ByteBuffer> byteBuffers = splitMOVPacket(frameBuffer, avcC);
		Picture pic = decoder.decodeFrameFromNals(byteBuffers, buf.getData());

		if (pic != null) {
			if(!frameDir.exists()){
				frameDir.mkdir();
			}
			
			byte[][] dataTemp = new byte[3][pic.getData().length];
			dataTemp[0] = pic.getPlaneData(0);
			dataTemp[1] = pic.getPlaneData(2);
			dataTemp[2] = pic.getPlaneData(1);
			Picture tmpBuf = Picture.createPicture(pixelWidth, pixelHeight, dataTemp, ColorSpace.YUV420J);
			transform.transform(tmpBuf, rgb);
			AWTUtil.toBufferedImage(rgb, renderImage);
			try {
				if(renderImage!=null) 
				ImageIO.write(renderImage, "jpg", new File(frameDir+File.separator+String.format("frame-%s.jpg", ++frameCounter)));
			}
			catch (IOException e) {
			}
		}
	}
	
	
	public  List<String>  getReport(String token,String tensor) throws  IOException, MkvElementVisitException, InterruptedException, ExecutionException, JSONException{

	String filePath =ApplicationConstants.FRAME_PATH+File.separator+token;
    List<String> listResult=new ArrayList<String>();
    CompletableFuture<Void> output=CompletableFuture.supplyAsync(() ->{
			String result=null;
			try {
				if(tensor.equalsIgnoreCase("SINGLE_OBJECT")) {
					while(new File(filePath+File.separator+"frame-"+(singleCounter)+".jpg").exists()) {
						TensorTemplate tensorTemplate=new TensorTemplate();
						
						File file = new File(filePath+File.separator+"frame-"+(singleCounter)+".jpg");
					    
						result="FrameTime: "+timeStamp(singleCounter)+", "+tensorTemplate.singleObjectTensor(file);
						System.out.println(result);
						listResult.add(result);
						final Path path = Paths.get(ApplicationConstants.REPORT_PATH+File.separator+tensor+"_"+token+".txt");
						Files.write(path, Arrays.asList(result), StandardCharsets.UTF_8,
						Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
						++singleCounter;
					}
				}
				else if(tensor.equalsIgnoreCase("MULTIPLE_OBJECT")){
					while(new File(filePath+File.separator+"frame-"+(doubleCounter)+".jpg").exists()) {
						TensorTemplate tensorTemplate=new TensorTemplate();
						File file = new File(filePath+File.separator+"frame-"+(singleCounter)+".jpg");
						result="FrameTime: "+timeStamp(doubleCounter)+", "+tensorTemplate.multipleObjectTensor(file);
						System.out.println(result);
						listResult.add(result);
						final Path path = Paths.get(ApplicationConstants.REPORT_PATH+File.separator+tensor+"_"+token+".txt");
						Files.write(path, Arrays.asList(result), StandardCharsets.UTF_8,
								Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
						++doubleCounter;
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return null;
		});
    return listResult;
	}
	
	     public static String timeStamp(long frameNo) {
	    	 long time=(1000/30)*frameNo;
	    	 long second = (time / 1000) % 60;
	    	 long minute = (time / (1000 * 60)) % 60;
	    	 long hour = (time / (1000 * 60 * 60)) % 24;
	    	 long milli=time-((hour*1000 * 60 * 60)+(minute*1000*60)+(second*1000));
	    	 return  String.format("%02d:%02d:%02d:%d", hour, minute, second, milli);
	     }
	     
	public  String getVideoToken() {
		return videoToken;
	}
	public String getTensorName() {
		return tensorName;
	}
	
}
