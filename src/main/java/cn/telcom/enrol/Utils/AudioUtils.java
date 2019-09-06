package cn.telcom.enrol.Utils;

import cn.telcom.enrol.config.response.ResponseTemplate;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioUtils {

	private static Logger logger = LoggerFactory.getLogger(AudioUtils.class);

	public static void main(String[] args) {
//		String s = audioToBase64("f://cms/voice/33_23.pcm");
//		System.out.println(s);
		Base64toAudio("f://r1.txt", "f://r1.wav");
	}

	public static String audioToBase64(String inPath) {

		byte[] readAudioData = AudioUtils.readAudioData(inPath);
		String byteArrayToBase64 = AudioUtils.byteArrayToBase64(readAudioData);
		return byteArrayToBase64;
	}

	public static String pcmToBase64(InputStream inputStream){
		byte[] readAudioData = readFile(inputStream);
		String byteArrayToBase64 = AudioUtils.byteArrayToBase64(readAudioData);
		return byteArrayToBase64;
	}


	/**
	 * 加入sox预处理音频
	 * @param inputStream
	 * @param pathAndFileName 文件路径和文件名
	 * @return
	 */
	public static  ResponseTemplate soxPreprocessingAudio(InputStream inputStream, String pathAndFileName){

		logger.info("当前的路径为：" + System.getProperty("user.dir"));//user.dir指定了当前的路径
		String replaceAll = pathAndFileName.replaceAll("/", "_");

		//临时存放音频文件名、目录
		String fileName = replaceAll + ".pcm";
		String path = System.getProperty("user.dir") + "/temp/";
		String pathAndName = path + fileName;
		File targetFile = new File(pathAndName);
		try {
			//先判断文件是否存在
			if (!targetFile.exists()){
				//不存在先创建目录
				File dir = new File(targetFile.getParent());
				if(!dir.exists()){
					boolean mkdirs = dir.mkdirs();
					if (!mkdirs){
						logger.error("创建保存文件夹失败");
						return ResponseTemplate.error("创建保存文件夹失败");
					}
				}
				boolean newFile = targetFile.createNewFile();
				if (!newFile){
					logger.error("创建保存文件失败");
					return ResponseTemplate.error("创建保存文件失败");
				}
			}
			OutputStream outStream = new FileOutputStream(targetFile);
			byte[] buff = new byte[1024];
			int rc = 0;
			while ((rc = inputStream.read(buff, 0, 1024)) > 0) {
				outStream.write(buff, 0, rc);
			}
			if (outStream != null){
				outStream.close();
			}
			try {
				ShellExcutor.callScript("./SoxPreprocessingAudio.sh", replaceAll);
			} catch (Exception e) {
				logger.error("shell脚本执行异常", e);
				return  ResponseTemplate.error("shell脚本执行异常");
			}
			String base64 = audioToBase64(path + replaceAll + ".wav");

			if (targetFile.delete() & new File("./temp/" + replaceAll + ".wav").delete()){
				logger.info("删除成功" + pathAndName);
			}else {
				logger.info("删除失败" + pathAndName);
			}
			return ResponseTemplate.ok(base64);

		} catch (IOException e) {
			logger.error("文件处理异常", e);
			return  ResponseTemplate.error("文件处理异常");
		}
	}



	public static byte[] readAudioData(String path) {

		int offset = path.toLowerCase().endsWith(".wav") ? 44 : 0;
		return readFile(path, offset);
	}

	public static byte[] readFile(String path) {
		return readFile(path, 0);
	}

	public static byte[] readFile(String path, int skip) {

		File file = new File(path);
		int fileLength = (int) file.length();
		if (skip >= fileLength)
			return null;
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			if (skip > 0) {
				in.skip(skip);
			}
			byte[] bytes = new byte[fileLength - skip];
			int length = in.read(bytes);
			return Arrays.copyOf(bytes, length);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static byte[] readFile(InputStream in) {
//		byte[] data = null;
		// 读取图片字节数组
		try {
			ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int rc = 0;
			while ((rc = in.read(buff, 0, 1024)) > 0) {
				swapStream.write(buff, 0, rc);
			}
			return swapStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}




	public static Pointer floatArrayToPointer(float[] array) {
		int floatSize = Native.getNativeSize(float.class);
		Pointer p = new Memory(array.length * floatSize);
		p.write(0, array, 0, array.length);
		return p;
	}

	public static Pointer[] float2DArrayToPointer(float[][] array) {
		int floatSize = Native.getNativeSize(float.class);

		Pointer[] data = new Pointer[array.length];
		for (int i = 0, k = array.length; i < k; i++) {
			float[] row = array[i];
			data[i] = new Memory(row.length * floatSize);
			data[i].write(0, row, 0, row.length);
		}
		return data;
	}

	public static void disposePointer(Pointer[] data) {
		for (Pointer p : data) {
			long peer = Pointer.nativeValue(p);
			Native.free(peer);
			Pointer.nativeValue(p, 0);
		}
	}

	public static Pointer[] byte2DArrayToPointer(byte[][] array) {
		int floatSize = Native.getNativeSize(byte.class);

		Pointer[] data = new Pointer[array.length];
		for (int i = 0, k = array.length; i < k; i++) {
			byte[] row = array[i];
			data[i] = new Memory(row.length * floatSize);
			data[i].write(0, row, 0, row.length);
		}
		return data;
	}

	public static void saveBinaryFile(String path, byte[] data) {
		File destFile = new File(path);
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(destFile);
			fileOutputStream.write(data);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static void saveToFile(String path, String content) {
		File destFile = new File(path);
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(destFile);
			fileOutputStream.write(content.getBytes("utf-8"));
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static String byteArrayToBase64(byte[] data) {
		String encode = new BASE64Encoder().encode(data);
		if (encode.contains("(\r\n|\r|\n|\n\r)")){
			encode = encode.replaceAll("(\r\n|\r|\n|\n\r)", "");
		}
		return encode;
	}

	public static byte[] base64ToByteArray(String base64) {
		try {
			return new BASE64Decoder().decodeBuffer(base64);
		} catch (IOException e) {
			return null;
		}
	}

	public static void saveVoicePrint(String path, float[] voicePrint) {
		File destFile = new File(path);
		if (!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		FileOutputStream fileOutputStream = null;
		FileChannel outChannel = null;
		try {
			fileOutputStream = new FileOutputStream(destFile);
			outChannel = fileOutputStream.getChannel();
			ByteBuffer buf = ByteBuffer.allocate(Float.SIZE * voicePrint.length / Byte.SIZE);
			buf.clear();
			buf.asFloatBuffer().put(voicePrint);
			outChannel.write(buf);
			buf.rewind();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (outChannel != null) {
				try {
					outChannel.close();
				} catch (Exception e) {
				}
			}
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static float[] readVoicePrint(String path) {

		FileInputStream fs = null;
		FileChannel inChannel = null;
		try {
			fs = new FileInputStream(path);

			int len = fs.available();
			float[] voicePrint = new float[len * Byte.SIZE / Float.SIZE];
			inChannel = fs.getChannel();
			ByteBuffer buf_in = ByteBuffer.allocate(len);
			buf_in.clear();
			inChannel.read(buf_in);
			buf_in.rewind();
			buf_in.asFloatBuffer().get(voicePrint);
			return voicePrint;
		} catch (IOException ex) {
			return null;
		} finally {
			if (inChannel != null) {
				try {
					inChannel.close();
				} catch (Exception e) {
				}
			}
			if (fs != null) {
				try {
					fs.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public byte[] fileToByte(String path) {
		int position = path.endsWith(".wav") ? 44 : 0;
		File file = new File(path);
		InputStream in = null;
		try {
			in = new FileInputStream(file);
			in.skip(position);
			byte[] bytes = new byte[in.available() - position];
			in.read(bytes);
			return bytes;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static byte[] floatArrayToByteArray(float[] vp) {
		ByteBuffer buf = ByteBuffer.allocate(Float.SIZE * vp.length / Byte.SIZE);
		buf.asFloatBuffer().put(vp);
		return buf.array();
	}

	public static float[] byteArrayToFloatArray(byte[] data) {
		float[] voicePrint = new float[data.length * Byte.SIZE / Float.SIZE];
		ByteBuffer buf_in = ByteBuffer.wrap(data);
		buf_in.asFloatBuffer().get(voicePrint);
		return voicePrint;
	}

	public static String readFile(File file) {

		if (file.isFile() && file.exists()) {
			FileInputStream fileInputStream = null;
			InputStreamReader inputStreamReader = null;
			BufferedReader bufferedReader = null;

			try {
				fileInputStream = new FileInputStream(file);
				inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
				bufferedReader = new BufferedReader(inputStreamReader);

				StringBuffer sb = new StringBuffer();
				String text = null;
				while ((text = bufferedReader.readLine()) != null) {
					sb.append(text);
				}
				return sb.toString();
			} catch (Exception e) {

			} finally {
				if (fileInputStream != null) {
					try {
						fileInputStream.close();
					} catch (Exception e) {
					}
				}
				if (inputStreamReader != null) {
					try {
						inputStreamReader.close();
					} catch (Exception e) {
					}
				}
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (Exception e) {
					}
				}
			}
		}
		return null;
	}

	public static void AudioToBase64(String inPath, String outPath) {
		byte[] readAudioData = readAudioData(inPath);

		String byteArrayToBase64 = byteArrayToBase64(readAudioData);

		saveToFile(outPath, byteArrayToBase64);
	}

	private static void Base64toAudio(String inPath, String outPath) {

		File file = new File(inPath);

		String readFile = readFile(file);

		byte[] readAudioData = readAudioData("path");

		byte[] base64ToByteArray = base64ToByteArray(readFile);

		saveBinaryFile(outPath, base64ToByteArray);
	}
}
