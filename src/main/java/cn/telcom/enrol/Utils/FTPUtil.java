package cn.telcom.enrol.Utils;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试连接ftp
 * @author   kenny_peng
 * @created  2019年8月19日
 */
public class FTPUtil {

    private static Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    /**
     * 登陆FTP并获取FTPClient对象
     *
     * @param host     FTP主机地址
     * @param port     FTP端口
     * @param userName 登录用户名
     * @param password 登录密码
     * @return
     */
    public static FTPClient loginFTP(String host, int port, String userName, String password) {
        FTPClient ftpClient = null;
        try {
            ftpClient = new FTPClient();
            // 连接FTP服务器
            ftpClient.connect(host, port);
            // 登陆FTP服务器
            ftpClient.login(userName, password);
            // 中文支持
            ftpClient.setControlEncoding("UTF-8");
            // 设置文件类型为二进制（如果从FTP下载或上传的文件是压缩文件的时候，不进行该设置可能会导致获取的压缩文件解压失败）
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                logger.error("连接FTP失败，用户名或密码错误。");
                ftpClient.disconnect();
            } else {
                logger.info("FTP连接成功!");
            }
        } catch (Exception e) {
            logger.error("登陆FTP失败，请检查FTP相关配置信息是否正确！", e);
        }
        return ftpClient;
    }

    /**
     * 从FTP下载文件到本地
     * @param ftpClient     已经登陆成功的FTPClient
     * @param ftpFilePath   FTP上的目标文件路径
//     * @param localFilePath 下载到本地的文件路径
     */
    public static void downloadFileFromFTP(FTPClient ftpClient, String ftpFilePath) {
        InputStream is = null;
//        FileOutputStream fos = null;
        try {
            // 获取ftp上的文件
            is = ftpClient.retrieveFileStream(ftpFilePath);

//            fos = new FileOutputStream(new File(localFilePath));

            String base64 = AudioUtils.pcmToBase64(is);
            System.out.println("readfile="+ base64.substring(0,80));

//             文件读取方式一
//            int i;
//            byte[] bytes = new byte[1024*1024*1024];
//            while ((i = is.read(bytes)) != -1) {
//                fos.write(bytes, 0, i);
//            }
            // 文件读取方式二
            //ftpClient.retrieveFile(ftpFilePath, new FileOutputStream(new File(localFilePath)));
            ftpClient.completePendingCommand();
            logger.info("FTP文件下载成功！" + ftpFilePath + "----inputstream.size():" + is.available());
        } catch (Exception e) {
            logger.error("FTP文件下载失败！" + ftpFilePath, e);
        } finally {
            try {
//                if (fos != null) {
//                    fos.close();
//                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取FTP某一特定目录下的所有文件名称
     * @param ftpClient     已经登陆成功的FTPClient
     * @param ftpDirPath    FTP上的目标文件路径
     */
    public static List<String> getFileNameListFromFTP(FTPClient ftpClient, String ftpDirPath) {
        List<String> stringList = new ArrayList<>();
        try {
            if (ftpDirPath.startsWith("/") && ftpDirPath.endsWith("/")) {
                // 通过提供的文件路径获取FTPFile对象列表
                FTPFile[] files = ftpClient.listFiles(ftpDirPath);
                // 遍历文件列表，打印出文件名称
                for (int i = 0; i < files.length; i++) {
                    FTPFile ftpFile = files[i];
                    // 此处只打印文件，未遍历子目录（如果需要遍历，加上递归逻辑即可）
                    if (ftpFile.isFile()) {
                        logger.info(ftpDirPath + ftpFile.getName());
                        stringList.add(ftpDirPath + ftpFile.getName());
                    }else {
                        List<String> strings = getFileNameListFromFTP(ftpClient, ftpDirPath + "/" + ftpFile.getName() + "/");
                        stringList.addAll(strings);
                    }
                }
            } else {
                logger.error("当前FTP路径不可用" + ftpDirPath);
            }
            return stringList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws IOException {

        //1、获取ftpclient
		FTPClient ftpClient = loginFTP("192.168.18.186", 21, "anonymous", null);

		//2、遍历给出的路径下所有文件路径+文件名集合
		List<String> stringPathList = getFileNameListFromFTP(ftpClient, "/pub/1/");
		List<InputStream> inputStreams = new ArrayList<>();
        //3、根据路径得到文件流
		int m = 0;
		for (String filePath : stringPathList) {
            ftpClient.changeWorkingDirectory(filePath);
//            String localPath = "f:\\cms" + filePath.replace("/", "\\");
//            File dir = new File(localPath.substring(0,localPath.lastIndexOf("\\")));
//            if (!dir.exists()){
//                dir.mkdirs();
//                File newFile = new File(localPath);
//                if (!newFile.exists()){
//                    newFile.createNewFile();
//                }
//            }
            new Thread(new Runnable() {
                @Override
                public void run() {

                }
            }).start();
            downloadFileFromFTP(ftpClient, filePath);
        }
//        for (InputStream inputStream : inputStreams){
//            //4、核心部分，处理文件流
//            System.out.println(inputStream);
//            String s = AudioUtils.pcmToBase64(inputStream);
//            System.out.println(s.substring(0,200));
//        }
	}
    
}
