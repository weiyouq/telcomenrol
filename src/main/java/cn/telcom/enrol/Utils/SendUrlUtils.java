package cn.telcom.enrol.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


/**
 * 发送URL请求工具类
 * @author: Administrator
 * @created:2019-04-11
 */
public class SendUrlUtils {
	
	public static void main(String[] args) {
//		sendGet("http://192.168.18.154:8080/processTxt?path=F://cms//1//4.txt");
		sendPost("http://192.168.18.154:8080/processTxt", "path=F://cms//1//4.txt");
	}
	
	/**
	 * HTTP GET请求
	 * @param url	请求地址
	 */
	public static String sendGet(String url) {

        try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			//默认值我GET
			con.setRequestMethod("GET");

			//添加请求头
			con.setRequestProperty("User-Agent", "Mozilla/5.0");

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
			    response.append(inputLine);
			}
			in.close();

			//打印结果
			return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

    }
	
	/**
	 * 发送post请求
	 * @param url   请求地址
	 * @param param	请求参数eg:"id=9"
	 * @return
	 */
	public static String sendPost(String url, String param){
		String result ="";
		BufferedReader in = null;
		PrintWriter out = null;
		
		try {
			URL realURL = new URL(url);
			
			//打开和url之间的连接
			URLConnection conn =realURL.openConnection();
			
			//设置通用参数
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
			
			//发送post请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			//获取URLConnection对象对应的输出流
			out = new PrintWriter(conn.getOutputStream());
			
			//发送请求参数
			out.print(param);
			
			//刷新输出流的缓存
			out.flush();
			
			//定义BufferedReader输入流来读取url的响应，输入结果编码形式为“UTF-8”
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line = in.readLine();
			while (line != null) {
				result = line + "\n";
			}
			return result;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
}
