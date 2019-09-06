package cn.telcom.enrol.Utils;

import cn.telcom.enrol.config.response.ResponseTemplate;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.net.ftp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
/** */

/**
 * 支持断点续传的FTP实用类
 * @version 0.1 实现基本断点上传下载
 * @version 0.2 实现上传下载进度汇报
 * @version 0.3 实现中文目录创建及中文文件创建，添加对于中文的支持
 */
public class ContinueFTP2{

//枚举类UploadStatus代码   

    public enum UploadStatus {
        Create_Directory_Fail,   //远程服务器相应目录创建失败
        Create_Directory_Success, //远程服务器闯将目录成功
        Upload_New_File_Success, //上传新文件成功
        Upload_New_File_Failed,   //上传新文件失败
        File_Exits,      //文件已经存在
        Remote_Bigger_Local,   //远程文件大于本地文件
        Upload_From_Break_Success, //断点续传成功
        Upload_From_Break_Failed, //断点续传失败
        Delete_Remote_Faild;   //删除远程文件失败
    }

    //枚举类DownloadStatus代码
    public enum DownloadStatus {
        Remote_File_Noexist, //远程文件不存在
        Local_Bigger_Remote, //本地文件大于远程文件
        Download_From_Break_Success, //断点下载文件成功
        Download_From_Break_Failed,   //断点下载文件失败
        Download_New_Success,    //全新下载文件成功
        Download_New_Failed;    //全新下载文件失败
    }

    public FTPClient ftpClient = new FTPClient();
    private Logger logger = LoggerFactory.getLogger(ContinueFTP2.class);

    private String ftpURL,username,pwd,ftpport,file1,file2;
    public ContinueFTP2(String _ftpURL, String _username, String _pwd, String _ftpport, String _file1){
        //设置将过程中使用到的命令输出到控制台     
        ftpURL = _ftpURL;
        username = _username;
        pwd = _pwd;
        ftpport = _ftpport;
        file1 = _file1;
        //打印ftp监听状态
//        this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
    }
    public ContinueFTP2(){};

    /** *//**
     * 连接到FTP服务器   
     * @param hostname 主机名   
     * @param port 端口   
     * @param username 用户名   
     * @param password 密码   
     * @return 是否连接成功
     * @throws IOException
     */
    public boolean connect(String hostname,int port,String username,String password) throws IOException{
        logger.info("----------开始连接ftp------------");
        ftpClient.connect(hostname, port);
        ftpClient.setControlEncoding("UTF-8");


        //由于apache不支持中文语言环境，通过定制类解析中文日期类型
//        ftpClient.configure(new FTPClientConfig("org.apache.commons.net.ftp.parser.UnixFTPEntryParser"));

        if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){
            if(ftpClient.login(username, password)){
                logger.info("----------连接ftp成功------------");
                return true;
            }
        }
        disconnect();
        logger.info("----------连接ftp失败------------");
        return false;
    }

    /** *//**
     * 从FTP服务器上下载文件,支持断点续传，上传百分比汇报   
     * @param remote 远程文件路径   
     * @return 上传的状态
     * @throws IOException
     */
    public Map<String,String> download(String remote){
        Map<String,String> resultMap = new HashMap<>();
        try {
            //设置被动模式
            ftpClient.enterLocalPassiveMode();
            //设置以二进制方式传输
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            DownloadStatus result;

            //检查远程文件是否存在
            FTPFile[] files = ftpClient.listFiles(remote);
            if(files.length != 1){
                System.out.println("远程文件不存在" + remote);
                resultMap.put("code","404");
                resultMap.put("msg", DownloadStatus.Remote_File_Noexist.toString());
                return resultMap;
            }

            //下载文件
            InputStream in= ftpClient.retrieveFileStream(new String(remote.getBytes("UTF-8"),"iso-8859-1"));


            //得到下载音频的base64
//            String base64 = AudioUtils.pcmToBase64(in);
            String base64 = "";
            ResponseTemplate responseTemplate = AudioUtils.soxPreprocessingAudio(in, remote);
            if ((int)responseTemplate.get("code") == 0){
                base64 = (String) responseTemplate.get("msg");
            }else{
                logger.error("音频："+ remote +"规格不符合要求，转换base64异常");
                resultMap.put("code", "500");
                resultMap.put("msg", "音频："+ remote +"规格不符合要求，转换base64异常");
                return resultMap;
            }
            in.close();
            boolean base64Result = ftpClient.completePendingCommand();

            //得到手机号
            String json = remote.substring(0, remote.lastIndexOf(".down.pcm")) + ".json";
//            System.out.println("remote="+ json);
            InputStream inJSON= ftpClient.retrieveFileStream(json);
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int rc = 0;
            while ((rc = inJSON.read(buff, 0, 1024)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            byte[] bytes = swapStream.toByteArray();
            Map<String,String> map = JSONObject.fromObject(new String(bytes));
            String callerid = map.get("callerid");//手机号
            String calledid = map.get("calledid");//用户呼叫的手机号，即呼叫的租户号码


            inJSON.close();
            boolean jsonResult = ftpClient.completePendingCommand();


            //成功complete则返回base64和手机号
            if(jsonResult && base64Result){

                resultMap.put("code","0");
                resultMap.put("base64", base64);
                resultMap.put("callerid", callerid);
                resultMap.put("calledid", calledid);
                return resultMap;
            }else {
                resultMap.put("code", "500");
                resultMap.put("msg", DownloadStatus.Download_New_Failed.toString());
                return resultMap;
            }
        } catch (IOException e) {
            logger.error("下载文件路径为："+ remote +"的文件出错",e);
            resultMap.put("code", "500");
            resultMap.put("msg", "下载文件路径为："+ remote +"的文件出错");
            return resultMap;
        }
    }

    /** *//**
     * 上传文件到FTP服务器，支持断点续传   
     * @param local 本地文件名称，绝对路径   
     * @param remote 远程文件路径，使用/home/directory1/subdirectory/file.ext或是 http://www.guihua.org /subdirectory/file.ext 按照Linux上的路径指定方式，支持多级目录嵌套，支持递归创建不存在的目录结构   
     * @return 上传结果
     * @throws IOException
     */
    public UploadStatus upload(String local,String remote) throws IOException{
        //设置PassiveMode传输     
        ftpClient.enterLocalPassiveMode();
        //设置以二进制流的方式传输     
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setControlEncoding("UTF-8");
        UploadStatus result;
        //对远程目录的处理     
        String remoteFileName = remote;
        if(remote.contains("/")){
            remoteFileName = remote.substring(remote.lastIndexOf("/")+1);
            //创建服务器远程目录结构，创建失败直接返回     
            if(CreateDirecroty(remote, ftpClient)== UploadStatus.Create_Directory_Fail){
                return UploadStatus.Create_Directory_Fail;
            }
        }

        //检查远程是否存在文件     
        FTPFile[] files = ftpClient.listFiles(new String(remoteFileName.getBytes("UTF-8"),"iso-8859-1"));
        if(files.length == 1){
            long remoteSize = files[0].getSize();
            File f = new File(local);
            long localSize = f.length();
            if(remoteSize==localSize){
                return UploadStatus.File_Exits;
            }else if(remoteSize > localSize){
                return UploadStatus.Remote_Bigger_Local;
            }

            //尝试移动文件内读取指针,实现断点续传     
            result = uploadFile(remoteFileName, f, ftpClient, remoteSize);

            //如果断点续传没有成功，则删除服务器上文件，重新上传     
            if(result == UploadStatus.Upload_From_Break_Failed){
                if(!ftpClient.deleteFile(remoteFileName)){
                    return UploadStatus.Delete_Remote_Faild;
                }
                result = uploadFile(remoteFileName, f, ftpClient, 0);
            }
        }else {
            result = uploadFile(remoteFileName, new File(local), ftpClient, 0);
        }
        return result;
    }
    /** *//**
     * 断开与远程服务器的连接   
     * @throws IOException
     */
    public void disconnect() throws IOException{
        if(ftpClient.isConnected()){
            ftpClient.disconnect();
        }
    }

    /** *//**
     * 递归创建远程服务器目录   
     * @param remote 远程服务器文件绝对路径   
     * @param ftpClient FTPClient 对象   
     * @return 目录创建是否成功
     * @throws IOException
     */
    public UploadStatus CreateDirecroty(String remote,FTPClient ftpClient) throws IOException{
        UploadStatus status = UploadStatus.Create_Directory_Success;
        String directory = remote.substring(0,remote.lastIndexOf("/")+1);
        if(!directory.equalsIgnoreCase("/")&&!ftpClient.changeWorkingDirectory(new String(directory.getBytes("UTF-8"),"iso-8859-1"))){
            //如果远程目录不存在，则递归创建远程服务器目录     
            int start=0;
            int end = 0;
            if(directory.startsWith("/")){
                start = 1;
            }else{
                start = 0;
            }
            end = directory.indexOf("/",start);
            while(true){
                String subDirectory = new String(remote.substring(start,end).getBytes("UTF-8"),"iso-8859-1");
                if(!ftpClient.changeWorkingDirectory(subDirectory)){
                    if(ftpClient.makeDirectory(subDirectory)){
                        ftpClient.changeWorkingDirectory(subDirectory);
                    }else {
                        System.out.println("创建目录失败");
                        return UploadStatus.Create_Directory_Fail;
                    }
                }

                start = end + 1;
                end = directory.indexOf("/",start);

                //检查所有目录是否创建完毕     
                if(end <= start){
                    break;
                }
            }
        }
        return status;
    }

    /** *//**
     * 上传文件到服务器,新上传和断点续传   
     * @param remoteFile 远程文件名，在上传之前已经将服务器工作目录做了改变   
     * @param localFile 本地文件 File句柄，绝对路径   
//     * @param processStep 需要显示的处理进度步进值
     * @param ftpClient FTPClient 引用   
     * @return
     * @throws IOException
     */
    public UploadStatus uploadFile(String remoteFile,File localFile,FTPClient ftpClient,long remoteSize) throws IOException{
        UploadStatus status;
        //显示进度的上传     
        long step = localFile.length() / 100;
        long process = 0;
        long localreadbytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localFile,"r");
        OutputStream out = ftpClient.appendFileStream(new String(remoteFile.getBytes("UTF-8"),"iso-8859-1"));
        //断点续传     
        if(remoteSize>0){
            ftpClient.setRestartOffset(remoteSize);
            process = remoteSize /step;
            raf.seek(remoteSize);
            localreadbytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        while((c = raf.read(bytes))!= -1){
            out.write(bytes,0,c);
            localreadbytes+=c;
            if(localreadbytes / step != process){
                process = localreadbytes / step;
                System.out.println("上传进度:" + process);
                //TODO 汇报上传状态     
            }
        }
        out.flush();
        raf.close();
        out.close();
        boolean result =ftpClient.completePendingCommand();
        if(remoteSize > 0){
            status = result? UploadStatus.Upload_From_Break_Success: UploadStatus.Upload_From_Break_Failed;
        }else {
            status = result? UploadStatus.Upload_New_File_Success: UploadStatus.Upload_New_File_Failed;
        }
        return status;
    }


    public Map<String, String> run() {
        try {
            this.connect(ftpURL, new Integer(ftpport), username, pwd);
            Map<String, String> downloadRsult = this.download(file1);
            this.disconnect();
            return downloadRsult;
        } catch (IOException e) {
            logger.error("连接FTP出错："+e.getMessage());
            return null;
        }
    }

    /**
     * 获取FTP某一特定目录下的所有文件名称
     * @param ftpDirPath    FTP上的目标文件路径
     */
    public List<String> getFileNameListFromFTP(String ftpDirPath, String ftpHostname, int ftpPort, String ftpUserName, String ftpPwd) {

        logger.info("获取ftp的路劲为："+ftpDirPath);
        List<String> stringList = new ArrayList<>();
        try {
//            logger.info("----------ftpDirPath------" + ftpDirPath + "---" + ftpDirPath.startsWith("/") + ftpDirPath.endsWith("/") + "---" + ftpDirPath.substring(ftpDirPath.length()-1,ftpDirPath.length()) + "---" + ftpDirPath.substring(0,1));
            if (ftpDirPath.startsWith("/") && ftpDirPath.endsWith("/")) {

                Vector<Thread> threadVector = new Vector<>();
                // 通过提供的文件路径获取FTPFile对象列表
                //先调用这个方法,这个方法的意思就是每次数据连接之前，ftp client告诉ftp server开通一个端口来传输数据。
                //因为ftp server可能每次开启不同的端口来传输数据，但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞。
                ftpClient.enterLocalPassiveMode();
                FTPFile[] files = ftpClient.listFiles(ftpDirPath);
                // 遍历文件列表，打印出文件名称
                for (int i = 0; i < files.length; i++) {
                    FTPFile ftpFile = files[i];
                    if (!ftpFile.isDirectory()) {
                        if (ftpFile.getName().endsWith(".down.pcm")) {
                            stringList.add(ftpDirPath + ftpFile.getName());
                        }
                    }else {

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ContinueFTP2 ftp2 = new ContinueFTP2();
                                try {
                                    ftp2.connect(ftpHostname, ftpPort, ftpUserName, ftpPwd);
                                } catch (IOException e) {
                                    logger.error("ftp连接异常", e);
                                }
                                List<String> strings = ftp2.getFtpNames(ftpDirPath + ftpFile.getName() + "/");
                                stringList.addAll(strings);
                            }
                        });
                        thread.start();
                        threadVector.add(thread);
                    }
                }
                for (Thread thread : threadVector){
                        thread.join();
                }
            } else {
                logger.error("当前FTP路径不可用" + ftpDirPath);
            }
            return stringList;
        } catch (IOException e) {
            logger.error("ftp读取文件异常", e);
            return null;
        } catch (InterruptedException e) {
            logger.error("子线程加入主线程异常", e);
            return null;
        }
    }

    public List<String> getFtpNames(String ftpDirPath) {

        logger.info("获取ftp的路劲为："+ftpDirPath);
        List<String> stringList = new ArrayList<>();
        try {
//            logger.info("----------ftpDirPath------" + ftpDirPath + "---" + ftpDirPath.startsWith("/") + ftpDirPath.endsWith("/") + "---" + ftpDirPath.substring(ftpDirPath.length()-1,ftpDirPath.length()) + "---" + ftpDirPath.substring(0,1));
            if (ftpDirPath.startsWith("/") && ftpDirPath.endsWith("/")) {

                // 通过提供的文件路径获取FTPFile对象列表
                //先调用这个方法,这个方法的意思就是每次数据连接之前，ftp client告诉ftp server开通一个端口来传输数据。
                //因为ftp server可能每次开启不同的端口来传输数据，但是在linux上，由于安全限制，可能某些端口没有开启，所以就出现阻塞。
                ftpClient.enterLocalPassiveMode();
                FTPFile[] files = ftpClient.listFiles(ftpDirPath);
                // 遍历文件列表，打印出文件名称
                for (int i = 0; i < files.length; i++) {
                    FTPFile ftpFile = files[i];
                    if (!ftpFile.isDirectory()) {
                        if (ftpFile.getName().endsWith(".down.pcm")) {
                            stringList.add(ftpDirPath + ftpFile.getName());
                        }
                    }else {
                        List<String> strings = getFtpNames(ftpDirPath + ftpFile.getName() + "/");
                        stringList.addAll(strings);
                    }
                }
            } else {
                logger.error("当前FTP路径不可用" + ftpDirPath);
            }
            return stringList;
        } catch (IOException e) {
            logger.error("ftp读取文件异常", e);
            return null;
        }
    }

    public static void main(String[] args) {
        ContinueFTP2 ftp2 = new ContinueFTP2();
        try {
            ftp2.connect("192.168.18.186", 21,"anonymous",null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> fileNameListFromFTP = null;//ftp2.getFileNameListFromFTP("/pub/1/");

        long l = System.currentTimeMillis();

        Vector<Thread> threadVector = new Vector<>();
        for (String s : fileNameListFromFTP){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Map<String, String> stringMap = new ContinueFTP2("192.168.18.186", "anonymous", null, "21", s).run();
                }
            });
            threadVector.add(t);
            t.start();
        }

        for (Thread t : threadVector){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(System.currentTimeMillis() - l);

    }
} 