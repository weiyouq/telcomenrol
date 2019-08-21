package cn.telcom.enrol.Utils;


import cn.telcom.enrol.bean.ReceiveVoiceInputObject;
import cn.telcom.enrol.config.response.ResponseTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 保存base64到文件，保存位置为：指定路径+当前日期+文件名(用户id.txt)
 * @author kenny_peng
 * @created 2019/7/25 11:04
 */
public class FileUtils {

    private static Logger   logger = LoggerFactory.getLogger(FileUtils.class);

    public static ResponseTemplate saveBase64(ReceiveVoiceInputObject inputObject, String voiceSaveLocation){

        byte[] bytes = inputObject.getBase64().getBytes();
        String date = formateDate("yyyyMMdd");

        //文件路径+文件名
        String filePathAndName = voiceSaveLocation + date + "/" + inputObject.getUserNo() + "_" + inputObject.getWhichDepartmentUse() + ".txt";

        if (null != bytes){
            try {
                File file = new File(filePathAndName);

                //先判断文件是否存在
                if (!file.exists()){
                    //不存在先创建目录
                    File dir = new File(file.getParent());
                    if(!dir.exists()){
                        boolean mkdirs = dir.mkdirs();
                        if (!mkdirs){
                            return ResponseTemplate.error("创建保存文件夹失败");
                        }
                    }
                    boolean newFile = file.createNewFile();
                    if (!newFile){
                        return ResponseTemplate.error("创建保存文件失败");
                    }
                }
                //创建文件输出流，将base64写入到文件
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(bytes);

                //关闭文件输出流
                outputStream.close();
                return ResponseTemplate.ok(filePathAndName);
            } catch (IOException e) {
                logger.error("文件读写异常",e);
                return ResponseTemplate.error("文件读写异常");
            }
        }else {
            logger.error("传入的base64为空");
            return ResponseTemplate.error("传入的base64为空");
        }
    }

    public static String formateDate(String formate){
        SimpleDateFormat sdf = new SimpleDateFormat(formate);
        String date = sdf.format(new Date());
        return date;
    }

    /**
     * 遍历路径path下的所有文件
     * @param file
     * @param listFiles
     */
    public static List<File> getAllFiles(File file, List<File> listFiles){
        if (file.exists()) {

            //判断文件是否是文件夹，如果是，开始递归
            if (!file.isFile()) {
                File f[] = file.listFiles();
                for (File file2 : f) {
                    getAllFiles(file2, listFiles);
                }
            }else {
                listFiles.add(file);
            }
        }
        return listFiles;
    }

    public static List<File> getFileList(File file) {

        List<File> listFiles = new ArrayList<>();
        if (file.exists()) {

            //判断文件是否是文件夹，如果是，开始递归
            if (!file.isFile()) {
                File f[] = file.listFiles();
                for (File file2 : f) {
                    getAllFiles(file2, listFiles);
                }
            }else {
                listFiles.add(file);
                logger.info("========="+file.getPath());
            }
        }
        return listFiles;

    }


    private void stringToFile(String s){
        FileWriter fw = null;
        File f = new File("f:\\cms\\.txt");
        try {
            if(!f.exists()){
                f.createNewFile();
            }
            fw = new FileWriter(f);
            BufferedWriter out = new BufferedWriter(fw);
            out.write(s, 0, s.length()-1);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
