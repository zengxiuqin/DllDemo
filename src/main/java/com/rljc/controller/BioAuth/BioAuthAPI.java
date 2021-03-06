package com.rljc.controller.BioAuth;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rljc.controller.BioAuth.module.ImageData;
import com.rljc.controller.BioAuth.module.RequestBody;
import com.rljc.controller.BioAuth.module.RequestBodyCloud;
import com.rljc.controller.BioAuth.module.ResponseBody;
import com.rljc.controller.BioAuth.module.ResponseBodyCloud;

import sun.misc.BASE64Encoder;

public class BioAuthAPI {
	
	private static Logger log = LoggerFactory.getLogger(BioAuthAPI.class);
	
	public static String appId = "com.paic.xface-acs";
	public static String appkey = "bioauthA399A626DAF04D05A6E01241A9BCF638";
	
	/**
	 * 将图片与本地库对比
	 * @param file1 图片一
	 * @param person_id 唯一身份 id
	 * @return
	 * @throws Exception
	 */
	public static ResponseBody compare(String file1,String person_id) throws Exception{
		BASE64Encoder base64 = new BASE64Encoder();
		RequestBody requestBodyEntity = new RequestBody(appId,person_id,"jpg");
		String[] imageData = new String[1];
		imageData[0] = byteArray2String(new BASE64Encoder().encode(readFile(file1)).getBytes("utf-8"));
		requestBodyEntity.setImageData(imageData);
		requestBodyEntity.setImageNum(1);
		String requestBody  = beanToJson(requestBodyEntity);
		try{
			String resposne = service("http://192.168.1.124:8080/bioauth/api01/face/faceDetect",requestBody);
			log.info("对比结果"+resposne);
			return jsonToBean(resposne, ResponseBody.class);
		}catch(Exception e){
			log.error("对比接口调用失败", e);
		}
		return null;
	}
	
	
	public static String service(String URL,String requestBody)throws Exception{
		
		URL url = new URL(URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod("POST");
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);
		long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
		int none = new Random().nextInt(9999999);
		connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
		String auth = SignatureUtil.getSignature(String.valueOf(timestamp),  String.valueOf(none));
		connection.setRequestProperty("Authorization", auth);
		log.info("Authorization:"+auth);
		connection.setRequestProperty("x-bi-timestamp",  String.valueOf(timestamp));
		log.info("x-bi-timestamp:"+String.valueOf(timestamp));
		connection.setRequestProperty("x-bi-none", String.valueOf(none));
		log.info("x-bi-none:"+String.valueOf(none));
		connection.setRequestProperty("x-bi-boundid", appId);
		log.info("x-bi-boundid:"+appId);
		connection.connect();
		DataOutputStream out = new DataOutputStream(connection.getOutputStream());
		out.writeBytes(requestBody);
		/*log.info("请求报文:"+requestBody);*/
		out.flush();
		out.close();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"utf-8"));
		String lines;
		
		StringBuffer sb = new StringBuffer("");
		while((lines = reader.readLine())!=null){
			lines = new String(lines.getBytes());
			sb.append(lines);
		}
		
		reader.close();
		connection.disconnect();
		return sb.toString();
		
	}
	
	public static byte[] readFile(String filePath){
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		File f = new File(filePath);
		int fSize = (int)f.length();
		byte[] buff = new byte[fSize];
//		byte[] buf = new byte[1024 * 50];
		InputStream in = null;
		try{
			in = new FileInputStream(filePath);
			int position = 0;
			while ((position = in.read(buff)) != -1){
				bos.write(buff, 0, position);
			}
			in.close();
			
			return bos.toByteArray();
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String byteArray2String(byte[] bs){
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < bs.length; i++){
			String inTmp = null;
			String text = Integer.toHexString(bs[i]);
			if(text.length() >= 2){
				inTmp = text.substring(text.length() - 2, text.length());
			}else{
				char[] array = new char[2];
				Arrays.fill(array, 0, 2 - text.length(), '0');
				System.arraycopy(text.toCharArray(), 0, array, 2 - text.length(), text.length());
				inTmp = new String(array);
			}
			sb.append(inTmp);
		}
		return sb.toString().toUpperCase();
	}
	
	/**
	 * 获取图片token
	 * @param imageStr 图片base64位数据
	 * @return
	 */
	public static String getTokenValue(String imageStr) {
		String hmacSha = null;
		String SIGNATURE_KEY = appkey;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec spec = new SecretKeySpec(SIGNATURE_KEY.getBytes("UTF-8"), "HmacSHA256");
			mac.init(spec);
			byte[] byteHMAC = mac.doFinal(imageStr.getBytes("UTF-8"));
			StringBuffer sbLogRet = new StringBuffer();
			for (int i = 0; i < byteHMAC.length; i++) {
				String inTmp = null;
				String text = Integer.toHexString(byteHMAC[i]);
				if (text.length() >= 2) {
					inTmp = text.substring(text.length() - 2, text.length());
				} else {
					char[] array = new char[2];
					Arrays.fill(array, 0, 2 - text.length(), '0');
					System.arraycopy(text.toCharArray(), 0, array, 2 - text.length(), text.length());
					inTmp = new String(array);
				}
				sbLogRet.append(inTmp);
			}
			hmacSha = sbLogRet.toString().toUpperCase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hmacSha;
	}

   public static String beanToJson(Object obj){
		String responseBody = "";
		try {
			ObjectMapper mapper = new ObjectMapper();
			//非空
			mapper.setSerializationInclusion(Include.NON_NULL); 
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
			mapper.setDateFormat(format);  
			responseBody = mapper.writeValueAsString(obj);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return responseBody;
	}
   
   /**
	 * 将json字符串转换为对象
	 * @param jsonString
	 * @param prototype
	 * @return
	 */
	public static  <T> T jsonToBean(String json, Class<T> prototype) {
	    ObjectMapper objectMapper = new ObjectMapper();
	    try {
	    	return (T) objectMapper.readValue(json, prototype);
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	    return null;
	}
	
	/**
	 * 根据上两张图片数据进行对比
	 * @param iamge1 待对比图片一
	 * @param image2 待对比图片二
	 * @throws Exception
	 */
	public static ResponseBodyCloud compare(ImageData image1,ImageData image2,String personId)throws Exception{
		RequestBodyCloud requestBody = new RequestBodyCloud(appId,personId);
		requestBody.setImage1(image1);
		requestBody.setImage2(image2);
		try{
			String reqBody  = beanToJson(requestBody);
		    String response = service("http://xface-stg.pingan.com.cn:130/bioauth/api01/face/compare", reqBody);
		    log.info(response);
		    return jsonToBean(response,ResponseBodyCloud.class);
		}catch(Exception e){
			log.error("对比接口出错", e);
		}
		return null;
	}
	
	public static ResponseBodyCloud compare(String image1,String image2,String personId)throws Exception{
		ImageData imageData1 = new ImageData(byteArray2String(image1.getBytes("utf-8")));
		ImageData imageData2 = new ImageData(byteArray2String(image2.getBytes("utf-8")));
		return compare(imageData1,imageData2,personId);
	}
	
	/**
	 * 
	 * @param image
	 * @return
	 * @throws Exception
	 */
	public static ResponseBodyCloud detect(String image,String personId) throws Exception{
		ImageData imageData1 = new ImageData(byteArray2String(image.getBytes("utf-8")));
		RequestBodyCloud requestBody = new RequestBodyCloud(appId,personId);
		requestBody.setImage(imageData1);
		String reqBody  = beanToJson(requestBody);
	    String response = service("http://xface-stg.pingan.com.cn:130/bioauth/api01/face/detect", reqBody);
	    log.info(response);
	    return jsonToBean(response,ResponseBodyCloud.class);
	}
	
	public static void main(String[] args) throws Exception {
		File file = new File("D://images//sample");
		for(String fi : file.list()){
			System.out.println(fi);
		}
		//System.out.println(getImageStr("D://images//u=1546987833,2182579858&fm=200&gp=0.jpg"));
	}
	
	public static String getImageStr(String imgFile) {
	    InputStream inputStream = null;
	    byte[] data = null;
	    try {
	        inputStream = new FileInputStream(imgFile);
	        data = new byte[inputStream.available()];
	        inputStream.read(data);
	        inputStream.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    // 加密
	    BASE64Encoder encoder = new BASE64Encoder();
	    return encoder.encode(data);
	}
	
}

