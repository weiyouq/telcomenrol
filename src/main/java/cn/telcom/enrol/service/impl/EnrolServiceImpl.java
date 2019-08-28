package cn.telcom.enrol.service.impl;


import cn.telcom.enrol.Utils.*;
import cn.telcom.enrol.bean.*;
import cn.telcom.enrol.config.Const;
import cn.telcom.enrol.config.response.ResponseTemplate;
import cn.telcom.enrol.dao.*;
import cn.telcom.enrol.service.IEnrolService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author kenny_peng
 * @created 2019/7/25 10:41
 */
@Service
@SuppressWarnings("unchecked")
public class EnrolServiceImpl implements IEnrolService {

    private int visitConut = 0;

    @Value("${voice.save.location.1vs1}")
    private String voiceSaveLocation;//1vs1音频保存位置

    @Value("${xs.vb.enrol.address}")
    private String enrolUrl;
    @Value("${xs.vb.verify.address}")
    private String verifyUrl;

    @Value("${xs.get.snr.score}")
    private String snrScore;
    @Value("${xs.vb.verify.score}")
    private String verifyScore;
    @Value("${xs.identity.enrol.address}")
    private String identifyEnrolAddress;

    @Value("${xs.yugao.ftp.hostname}")
    private String ftpHostname;
    @Value("${xs.yugao.ftp.port}")
    private String ftpPort;
    @Value("${xs.yugao.ftp.userName}")
    private String ftpUserName;
    @Value("${xs.yugao.ftp.password}")
    private String ftpPwd;

    /**
     * 均衡url
     * @param url   传入请求url
     * @return
     */
    private String getUseUrl(String url){
        String[] urls = url.split(",");
        visitConut++;
        int num = visitConut%urls.length;
        return urls[num];

    }

    @Value("${xs.max.thread.count}")
    private int MAX_THREAD_COUNT;

    private volatile int completedThread = MAX_THREAD_COUNT;
    private synchronized int getCompletedThreadCount() {
        return completedThread;
    }
    private synchronized void setCompletedThreadCount(int val) {
        completedThread = completedThread + val;
    }

    private List<String> needToVBEnrolFiles = new ArrayList<>();//一对一需要注册的音频路径
    private List<String> needToIdentifyEnrolFiles = new ArrayList<>();//一对多需要注册的音频

    @Autowired
    private IUserDao userDao;
    @Autowired
    private IUserBusinessDao userBusinessDao;
    @Autowired
    private IBusinessDao businessDao;
    @Autowired
    private IActivityLogDao activityLogDao;
    private Logger logger = LoggerFactory.getLogger(EnrolServiceImpl.class);

    /**
     * 1vs1 声纹注册，
     * 1、先读取数据库t_1vs1_enrol表，查找注册状态为0的信息，（每次多少条）
     * 2、进行注册，更新t_1vs1_enrol表注册结果，
     * @return
     */
    @Override
    public String toEnrolSpeaker() {
        needToVBEnrolFiles.clear();
        //1、根据音频存储路径，获取所有音频文件
        String nowDayFilePath = voiceSaveLocation  + FileUtils.formateDate("yyyyMMdd") + "/";//File.separator;
//        File file = new File(nowDayFilePath);
        //获取所有文件的路径
        List<String> fileNameListFromFTP = accordingPathGetAllFilePath(nowDayFilePath);
//        List<File> listFiles = FileUtils.getFileList(file);
        logger.info("查询文件路径nowDayFilePath：" + nowDayFilePath + "------" + "得到的文件集合大小listFiles.size()：" + fileNameListFromFTP.size());

        List<String> alreadyEnroledList = activityLogDao.selectAlreadyEnroledList();
        List<ResponseTemplate> resultList = new ArrayList<>();
        for (String singleFileString : fileNameListFromFTP){
            if (!alreadyEnroledList.contains(singleFileString)){
                needToVBEnrolFiles.add(singleFileString);
//                vbEnrolProcessMethod(singleFile);
            }
        }
        //多线程启动注册
        start(resultList, 0);
        return JSONArray.fromObject(resultList).toString();
    }

    /**
     * //多线程启动注册
     * @param resultList    返回集合
     * @param taskName      执行一对多或者一对一任务
     */
    private Object start(Object resultList, int taskName){
//        long l = System.currentTimeMillis();
        List<String> success = new ArrayList<>();
        List<String> error = new ArrayList<>();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Vector<Thread> threadVector = new Vector<>();
                for (int i = 0; i < MAX_THREAD_COUNT; i++) {
                    Thread threadChildren = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //执行任务
                            if (taskName == 0){//一对一任务
                                doVbEnrollWork((List<ResponseTemplate>) resultList);
                            }else if (taskName == 1){//一对多任务
                                doIdentifyWork(success, error);
                            }
                        }
                    });
                    threadVector.add(threadChildren);
                    threadChildren.start();
                }
                for (Thread thread1 : threadVector){
                    try {
                        thread1.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //如果线程数满了，等待
                waitThread();
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            logger.error("线程加入到主线程异常",e);
        }
        Map<String, List<String>> map = new HashMap<>();
        if (taskName == 1){

            map.put("success",success);
            map.put("error",error);
        }
        return map;
    }

    /**
     * 执行一对一注册任务
     * @param resultList
     */
    private void doVbEnrollWork(List<ResponseTemplate> resultList) {
        String filePath = null;
        while ((filePath=popAudioItem(0)) != null) {
            ResponseTemplate responseTemplate = vbEnrolProcessMethod(filePath);
            resultList.add(responseTemplate);
        }
        setCompletedThreadCount(-1);//结束循环用
    }

    /**
     * 执行一对多注册任务
     * @param success
     * @param error
     */
    private void doIdentifyWork(List<String> success, List<String> error) {
        String filePath = null;
        while ((filePath=popAudioItem(1)) != null) {
            //进行注册
            String enrolResult = identifyEnrolSpeaker(filePath);
            Map<String, Object> map = JSONObject.fromObject(enrolResult);

            //如果包含retCode，注册成功，否则、注册失败
            if (map.containsKey("retCode")){
                success.add(filePath);
            }else{
                error.add(filePath + enrolResult);
            }
        }
        setCompletedThreadCount(-1);//结束循环用
    }

    /**
     * 获取需要注册的file对象
     * @return
     */
    private synchronized String popAudioItem(int taskName) {
        if (taskName == 0 ){
            if (needToVBEnrolFiles.size() == 0)
                return null;
            String item = needToVBEnrolFiles.get(0);
            needToVBEnrolFiles.remove(0);
            return item;
        }else {
            if (needToIdentifyEnrolFiles.size() == 0)
                return null;
            String item = needToIdentifyEnrolFiles.get(0);
            needToIdentifyEnrolFiles.remove(0);
            return item;
        }
    }

    /**
     * 1对多声纹注册
     * @return
     */
    @Override
    public String identifyService(String filePath) {
//        File file = new File(filePath);
//        if (file.isDirectory()){
//        if (!file.isFile()){
        needToIdentifyEnrolFiles.clear();
        //如果filePath是一个目录，遍历得到所有文件
        needToIdentifyEnrolFiles = accordingPathGetAllFilePath(filePath);
        logger.info("查询文件路径filePath：" + filePath + "--------" + "得到的文件集合大小needToIdentifyEnrolFiles.size()：" + needToIdentifyEnrolFiles.size());

        if (needToIdentifyEnrolFiles.size() == 0){
            return JSONObject.fromObject(ResponseTemplate.error("选择目录为空，请重新选择!")).toString();
        }else {
            Map<String, List<String>> map = new HashMap<>();
            Object start = start(map, 1);
            return JSONObject.fromObject(start).toString();
        }
//        }else {
//            //如果file,则针对该文件进行注册
//            String enrolSpeaker = identifyEnrolSpeaker(filePath);
//            return enrolSpeaker;
//        }
    }

    private String identifyEnrolSpeaker(String filePath) {
        Map<String, String> stringMap = new ContinueFTP2(ftpHostname, ftpUserName, ftpPwd, ftpPort, filePath).run();
        //返回值为0，下载成功
        if (stringMap.get("code").equals("0")){

            String enrolBase64 = stringMap.get("base64");
            String fileName = stringMap.get("callerid")+ "_null_null.pcm";
            String[] names = getUserArray(fileName);
            if (names != null){

                //1、先进性1对1验证，判断性噪比
//                ResponseTemplate passSNR = isPassSNR("log-" + FileUtils.formateDate("yyyyMMddHHmmssSSS"), stringMap.get("callerid"), enrolBase64);//log日志名)
//                if ((Integer) passSNR.get("code") == 0){








                    String enrolPayload = PayloadUtils.identifyPayload(names[0], enrolBase64);
                    String enrolResult = PostRequest.sendPost(getUseUrl(identifyEnrolAddress), enrolPayload);

                    User user = userDao.selectUserByNoAndEnrolCategory(names[0], 1);
                    boolean booleanUserBuNo = names[1].equals("null");
                    Business business = businessDao.selectByBuNo(names[1]);
                    int buID;

                    long userID;//= user.getId();
                    if (user == null){
                        User newUser = new User(names[0],1,null,null);
                        userDao.insertSelective(newUser);
                        userID = newUser.getId();
                        if (!booleanUserBuNo){
                            if (business == null){
                                Business newBusiness = new Business(names[1]);
                                businessDao.insertSelective(newBusiness);
                                business = newBusiness;
                            }
                        }
                    }else {
                        userID = user.getId();
                    }
                    if (!booleanUserBuNo){
                        UserBusiness userBusiness = userBusinessDao.selectUserByUserIdAndBuId(userID, business.getId());
                        if (userBusiness == null){
                            userBusinessDao.insertSelective(new UserBusiness(userID,business.getId()));
                        }
                    }
                    if (business == null){
                        if (enrolResult.length() > 200){
                            activityLogDao.insertSelective(new ActivityLog(userID,null,filePath,1001,new Date(),enrolResult.substring(0,200)));
                        }else {
                            activityLogDao.insertSelective(new ActivityLog(userID,null,filePath,1001,new Date(),enrolResult));
                        }
                    }else {
                        if (enrolResult.length() > 200){
                            activityLogDao.insertSelective(new ActivityLog(userID,business.getId(),filePath,1001,new Date(),enrolResult.substring(0,200)));
                        }else {
                            activityLogDao.insertSelective(new ActivityLog(userID,business.getId(),filePath,1001,new Date(),enrolResult));
                        }
                    }

                    return enrolResult;
//                }else {
//                    return JSONObject.fromObject(passSNR).toString();
//                }
            }else{
                return JSONObject.fromObject(ResponseTemplate.error("注册音频命名不规范，请按照要求命名")).toString();
            }
        }else {//下载失败
            return JSONObject.fromObject(ResponseTemplate.error("路径：“" + filePath + "”的文件下载失败" + stringMap.get("msg"))).toString();
        }
    }
    private String[] getUserArray(String fileName){
        String[] names = fileName.split("_");
        if (names.length >=3){
            String userNo = names[0];
            String zuhu = names[1];

            String blackOrWrite = names[2].substring(0,names[2].indexOf("."));
            names = new String[]{userNo, zuhu, blackOrWrite};
            return names;
        }else{
            return null;
        }
    }

    /**
     *根据EnrolModel，base64进行注册（包含两次注册功能）
     * @param filePath          文件对象
     * @return
     */
    private ResponseTemplate vbEnrolProcessMethod(String filePath) {
        Map<String, String> stringMap = new ContinueFTP2(ftpHostname, ftpUserName, ftpPwd, ftpPort, filePath).run();
        //返回值为0，下载成功
        if (stringMap.get("code").equals("0")){
            String base64 = stringMap.get("base64");
            String logName = "log-"+ FileUtils.formateDate("yyyyMMddHHmmssSSS");//log日志名
//            String fileName = filePath.substring(filePath.lastIndexOf(File.separator)+1, filePath.length());
            String userNo = stringMap.get("callerid");
//            String fileName = userNo+ "_null.pcm";
            String bu_no = "null";//fileName.substring(fileName.indexOf("_")+1, fileName.lastIndexOf("."));

            //1、查询用户是存在
            User user = userDao.selectUserByNoAndEnrolCategory(userNo, 0);
            Business business = businessDao.selectByBuNo(bu_no);

            if(user == null){
                //1、声纹预备注册
                String userNo_prepare = userNo + "_2";
                ResponseTemplate responseTemplate = enrolSpeaker(logName, userNo_prepare, base64);
                if ((int)responseTemplate.get("code") == 0){//注册成功
                    User newUser = new User();
                    newUser.setUserNo(userNo);
                    newUser.setEnrolCategory(0);
                    newUser.setVpCount(1);
                    userDao.insertSelective(newUser);

//                int buId = Integer.parseInt(null);
                    if (!bu_no.equals("null")){
                        if (business == null){
                            Business newBusiness = new Business();
                            newBusiness.setBuNo(bu_no);
                            businessDao.insertSelective(newBusiness);
                            business = newBusiness;
                        }
                        userBusinessDao.insertSelective(new UserBusiness(newUser.getId(), business.getId()));
                    }
                    recordVBEnrolledLog(business,newUser.getId(), filePath,Const.VB_ENROL, new Date(),objToStr(responseTemplate));
                }else{
                    recordVBEnrolledLog(business,null, filePath,Const.VB_ENROL_FAILED, new Date(),objToStr(responseTemplate));
                }
                return responseTemplate;
            }else {
                //添加用户租户关系
                if (!bu_no.equals("null")){
                    if (business == null){
                        business = new Business(bu_no);
                        businessDao.insertSelective(business);
                    }
                    userBusinessDao.selectUserByUserIdAndBuId(user.getId(),business.getId());
                    if (userBusinessDao == null){
                        userBusinessDao.insertSelective(new UserBusiness(user.getId(),business.getId()));
                    }
                }
                //判断用户注册数量
                if (user.getVpCount() == 1){
                    if (user.getVerifyNo() == null  ||  user.getVerifyNo().equals("")){//说明有的一条模型是预注册

                        //判断当天是否注册过，注册过就结束
                        List<ActivityLog> activityLogList = activityLogDao.selectIfTodayEnrolled(user.getId(), FileUtils.formateDate("yyyyMMdd"));
                        if (activityLogList.size() >0){
                            //结束
                            ResponseTemplate nowDayEnrolled = ResponseTemplate.error(userNo + "今天已经注册过，不在进行注册流程!");
                            recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_NOW_DAY_ENROLED, new Date(), objToStr(nowDayEnrolled));
                            return ResponseTemplate.error(userNo + "今天已经注册过，不在进行注册流程!");
                        }else{
                            //1、跟预注册声纹模型进行验证
                            ResponseTemplate responseTemplate = verifySpeaker(logName, userNo + "_2", base64);
                            if ((int)responseTemplate.get("code") == 0){
                                recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_VERIFY,new Date(), objToStr(responseTemplate));
                                if ((boolean)responseTemplate.get("msg") == true){//验证通过
                                    userDao.updateByPrimaryKeySelective(new User(user.getId(),userNo + "_2",1));
                                    return responseTemplate;
                                }else {
                                    //1、声纹注册（预注册）
                                    ResponseTemplate responseTemplate1 = enrolSpeaker(logName, userNo + "_22", base64);
                                    String ss1 = JSONObject.fromObject(responseTemplate1).toString();
                                    if ((int)responseTemplate1.get("code") == 0){

                                        //注册成功更新user状态
                                        userDao.updateByPrimaryKeySelective(new User(user.getId(), 2));
                                        recordVBEnrolledLog(business, user.getId(),filePath,Const.VB_ENROL,new Date(),ss1);
                                    }else {//注册失败
                                        recordVBEnrolledLog(business, user.getId(),filePath,Const.VB_ENROL_FAILED,new Date(),ss1);
                                    }
                                    return responseTemplate1;
                                }
                            }else{
                                recordVBEnrolledLog(business, user.getId(),filePath, Const.VB_ENROL_FAILED, new Date(), objToStr(responseTemplate));
                                return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate);
                            }
                        }
                    }else {//user.getVerify_no（）不为空，说明有的一条模型是激活的模型

                        //预注册模型的用户编号
//                        String prepareEnrolNo = getPrepareEnrolNo(user.getVerifyNo());
                        //1、跟激活的模型进行验证
                        ResponseTemplate responseTemplate = verifySpeaker(logName, user.getVerifyNo(), base64);
                        if ((int)responseTemplate.get("code") == 0){
                            //1、添加验证记录
                            recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_VERIFY, new Date(), objToStr(responseTemplate));
                            if ((boolean) responseTemplate.get("msg")) {//验证通过
                                return ResponseTemplate.ok(filePath + "注册成功");
                            }else {//验证不通过,将激活模型设置为验证关注，vpcount=0就是验证关注

                                //将激活模型设置为验证关注
                                userDao.updateByPrimaryKeySelective(new User(user.getId(), 0));
                                return responseTemplate;
                            }
                        }else {
                            recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_ENROL_FAILED, new Date(), objToStr(responseTemplate));
                            return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate);
                        }
                    }
                }else if (user.getVpCount() == 2){//有两条模型,说明模型有初始状态1，和初始状态2，

                    //跟初始状态1进行验证
                    ResponseTemplate responseTemplate = verifySpeaker(logName, userNo + "_2", base64);
                    if ((int)responseTemplate.get("code") == 0){
                        if ((boolean)responseTemplate.get("msg") == true) {//验证通过
                            //将初始状态1设为激活模型
                            userDao.updateByPrimaryKeySelective(new User(user.getId(),userNo + "_2",1));
                            return ResponseTemplate.ok(filePath + "注册成功");
                        }else {//验证不通过，跟初始状态2进行比对
                            ResponseTemplate responseTemplate_22 = verifySpeaker(logName, userNo + "_22", base64);

                            if ((int)responseTemplate_22.get("code") == 0) {
                                //add验证记录表
                                recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_VERIFY, new Date(), objToStr(responseTemplate_22));
                                if ((boolean) responseTemplate_22.get("msg") == true) {//验证通过
                                    //将初始状态1设为激活模型
                                    userDao.updateByPrimaryKeySelective(new User(user.getId(),userNo + "_22",1));
                                    return ResponseTemplate.ok(filePath + "注册成功");
                                }else {
                                    return ResponseTemplate.ok(filePath + "声纹比对不通过，不进行操作！");
                                }
                            }else{
                                //add验证记录表
                                recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_VERIFY_FAILED, new Date(), objToStr(responseTemplate_22));
                                return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate_22);
                            }


//                            //预注册模型的用户编号
//                            String prepareEnrolNo = getPrepareEnrolNo(user.getVerifyNo());
//                            //1、跟预注册声纹模型进行验证
//                            ResponseTemplate responseTemplate2 = verifySpeaker(logName, prepareEnrolNo, base64);
//                            if ((int)responseTemplate2.get("code") == 0){
//                                if ((boolean)responseTemplate2.get("msg") == true) {//验证通过
//                                    userDao.updateByPrimaryKeySelective(new User(user.getId(), prepareEnrolNo,1));
//                                    if (JSONObject.fromObject(responseTemplate2).toString().length() > 200){
//                                        activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), filePath, 3, new Date(),JSONObject.fromObject(responseTemplate2).toString().substring(0,200)));
//                                    }else {
//                                        activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), filePath, 3, new Date(),JSONObject.fromObject(responseTemplate2).toString()));
//
//                                    }
//                                    return ResponseTemplate.ok(filePath + "注册成功");
//                                }else {
//                                    //1、声纹注册（预注册）
//                                    ResponseTemplate responseTemplate1 = enrolSpeaker(logName, prepareEnrolNo, base64);
//                                    String res = JSONObject.fromObject(responseTemplate1).toString();
//                                    if (res.length() > 200){
//                                        activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), filePath, 1, new Date(),res.substring(0,200)));
//                                    }else {
//                                        activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), filePath, 1, new Date(),res));
//                                    }
//                                    return ResponseTemplate.ok(filePath + "注册成功");
//                                }
//                            }else {
//                                return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate2);
//                            }
                        }
                    }else {
                        return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate);
                    }
                }else if(user.getVpCount() == 0) {//模型条数为0，则为注册关注

                    ResponseTemplate responseTemplate = verifySpeaker(logName, user.getVerifyNo(), base64);

                    if ((int)responseTemplate.get("code") == 0) {
                        recordVBEnrolledLog(business, user.getId(),filePath, Const.VB_VERIFY,new Date(), objToStr(responseTemplate));
                        if ((boolean) responseTemplate.get("msg")) {//验证通过
                            //将注册关注设为激活模型
                            userDao.updateByPrimaryKeySelective(new User(user.getId(), 1));
                            return ResponseTemplate.ok(filePath + "注册成功");
                        } else {//验证不通过，删除注册关注
                            int i2 = userDao.deleteByPrimaryKey(user.getId());
                            int i1 = 1;
                            int i = 1;
                            if (business != null){
                                i1 = businessDao.deleteByPrimaryKey(business.getId());
                                i = userBusinessDao.deleteByUserIdAndBuId(user.getId(), business.getId());
                            }
                            if(i>0 && i1>0 && i2>0){
                                return ResponseTemplate.ok("注册关注匹配通过，删除注册关注成功");
                            }else {
                                return ResponseTemplate.ok("注册关注匹配不通过，删除注册关注失败");
                            }
                        }
                    }else {
                        recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_ENROL_FAILED, new Date(), objToStr(responseTemplate));
                        return ResponseTemplate.error(filePath + "注册失败"+ responseTemplate);
                    }
                }else {
                    ResponseTemplate vpCountError = ResponseTemplate.error("声纹模型数量异常");
                    recordVBEnrolledLog(business, user.getId(), filePath, Const.VB_ENROL_FAILED, new Date(), objToStr(vpCountError));
                    return vpCountError;
                }
            }
        }else {//下载失败
            recordVBEnrolledLog(null, null, filePath, Const.DOWNLOAD_FAILED, new Date(), stringMap.get("msg"));
            return ResponseTemplate.error("路径：“" + filePath + "”的文件下载失败" + stringMap.get("msg"));
        }
    }

    /**
     * 记录1vs1声纹注册记录
     * @param business
     * @param userId
     * @param voiceLocation
     * @param category
     * @param createDate
     * @param result
     * @return
     */
    private int recordVBEnrolledLog(Business business,Long userId, String voiceLocation, Integer category, Date createDate, String result){
        int insertSelective;
        if (business == null){
            if (result.length() > 200){
                insertSelective = activityLogDao.insertSelective(new ActivityLog(userId, null, voiceLocation, category, createDate, result.substring(0,200)));
            }else {
                insertSelective = activityLogDao.insertSelective(new ActivityLog(userId, null, voiceLocation, category, createDate, result));
            }
        }else {
            if (result.length() > 200){
                insertSelective = activityLogDao.insertSelective(new ActivityLog(userId, business.getId(), voiceLocation, category, createDate, result.substring(0,200)));
            }else {
                insertSelective = activityLogDao.insertSelective(new ActivityLog(userId, business.getId(), voiceLocation, category, createDate, result));
            }
        }
        return insertSelective;
    }

    /**
     * 对象转json字符串
     * @param o
     * @return
     */
    private String objToStr(Object o){
        return JSONObject.fromObject(o).toString();
    }

    /**
     * 预注册模型的用户编号
     * @param verifyNo  查询出来的的验证编号
     * @return
     */
    private String getPrepareEnrolNo(String verifyNo){
//        String verifyNo = user.getVerifyNo();
        String prepareEnrolNo = null;//预注册模型的用户编号
        if (verifyNo.contains("_")){
            String houzui = verifyNo.substring(verifyNo.lastIndexOf("_"),verifyNo.length());
            if (houzui.equals("_2")){
                prepareEnrolNo = verifyNo.substring(0,verifyNo.indexOf(houzui));
            }else {
                prepareEnrolNo = verifyNo + "_2";
            }
        }else {
            prepareEnrolNo = verifyNo + "_2";
        }
        return prepareEnrolNo;
    }

    /**
     * 根据userNo，base64进行注册
     * @param logName   日志名
     * @param userNo    用户编号
     * @param base64    音频对应base64
     * @return
     */
    private ResponseTemplate enrolSpeaker(String logName, String userNo, String base64){
        //得到声纹注册payload
        String params = PayloadUtils.enrolOrVerifyPayload(logName,userNo,base64);

        //发送post请求,调用声纹引擎注册接口，得到声纹注册结果
        String enrolResult = PostRequest.sendPost(getUseUrl(enrolUrl), params);
        Map<String, Object> enrolResultMap = JSONObject.fromObject(enrolResult);
        //返回正确结果
        if (enrolResultMap.containsKey("result")){

            //解析verifyResult，得到信噪比值
            Object o = JSONObject.fromObject(enrolResultMap.get("result")).get("metaInformation");
            JSONArray jsonArray = JSONArray.fromObject(o);
            String resultSnrScore = null;
            for (int i = 0; i<jsonArray.size(); i++){
                if (JSONObject.fromObject(jsonArray.getString(i)).get("key").equals("get-snr")){
                    resultSnrScore = (String) JSONObject.fromObject(JSONObject.fromObject(jsonArray.getString(i)).get("value")).get("value");
                }
            }

            //如果信噪比分数低于阈值，返回错误
            if (Double.valueOf(resultSnrScore) < Double.valueOf(snrScore)){
                logger.error("分数低于阈值");
                return ResponseTemplate.error("分数低于阈值");
            }else {
                return ResponseTemplate.ok("预注册成功");
            }
        }else {//返回错误结果
            return ResponseTemplate.error(enrolResult);
        }
    }

    /**
     * 根据userNo，用户传入音频的base64进行验证
     * @param logName   日志名称
     * @param userNo    用户编号
     * @param base64    用户传入音频对应的base64
     * @return
     */
    private ResponseTemplate verifySpeaker(String logName, String userNo, String base64){

        //得到声纹验证payload
        String verifyPayload = PayloadUtils.enrolOrVerifyPayload(logName,userNo,base64);

        //发送post请求,调用声纹引擎验证接口，得到声纹验证结果
        String verifyResult = PostRequest.sendPost(getUseUrl(verifyUrl),verifyPayload);
        Map<String,Object> verifyResultMap = JSONObject.fromObject(verifyResult);

        //返回正确结果
        if (verifyResultMap.containsKey("result")){

            //解析verifyResult，得到信噪比值
            Object o = JSONObject.fromObject(verifyResultMap.get("result")).get("metaInformation");
            JSONArray jsonArray = JSONArray.fromObject(o);
            String resultSnrScore = null;
            for (int i = 0; i<jsonArray.size(); i++){
                if (JSONObject.fromObject(jsonArray.getString(i)).get("key").equals("get-snr")){
                    resultSnrScore = (String) JSONObject.fromObject(JSONObject.fromObject(jsonArray.getString(i)).get("value")).get("value");
                }
            }

            //如果信噪比分数低于阈值，返回错误
            if (Double.valueOf(resultSnrScore) < Double.valueOf(snrScore)){
                return ResponseTemplate.error("分数低于阈值");
            }else {
                String getVerifyScore = (String) JSONObject.fromObject(verifyResultMap.get("result")).get("score");

                //符合验证得分小于阈值返回false，否则返回true
                if (Double.valueOf(getVerifyScore) > Double.valueOf(verifyScore)){
                    return ResponseTemplate.ok(true);
                }else {
                    return ResponseTemplate.ok(false);
                }
            }
            /*String getVerifyScore = (String) JSONObject.fromObject(verifyResultMap.get("result")).get("score");
            //符合验证得分小于阈值返回false，否则返回true
            if (Double.valueOf(getVerifyScore) > Double.valueOf(verifyScore)){
                return ResponseTemplate.ok(true);
            }else {
                return ResponseTemplate.ok(false);
            }*/
        }else {//返回错误结果
            return ResponseTemplate.error(verifyResult);
        }
    }


    /**
     * snr值检测
     * @param logName
     * @param userNo
     * @param base64
     * @return
     */
    private ResponseTemplate isPassSNR(String logName, String userNo, String base64){
        //得到声纹验证payload
        String verifyPayload = PayloadUtils.enrolOrVerifyPayload(logName,userNo,base64);

        //发送post请求,调用声纹引擎验证接口，得到声纹验证结果
        String verifyResult = PostRequest.sendPost(getUseUrl(verifyUrl),verifyPayload);
        Map<String,Object> verifyResultMap = JSONObject.fromObject(verifyResult);

        //返回正确结果
        if (verifyResultMap.containsKey("result")){

            //解析verifyResult，得到信噪比值
            Object o = JSONObject.fromObject(verifyResultMap.get("result")).get("metaInformation");
            JSONArray jsonArray = JSONArray.fromObject(o);
            String resultSnrScore = null;
            for (int i = 0; i<jsonArray.size(); i++){
                if (JSONObject.fromObject(jsonArray.getString(i)).get("key").equals("get-snr")){
                    resultSnrScore = (String) JSONObject.fromObject(JSONObject.fromObject(jsonArray.getString(i)).get("value")).get("value");
                }
            }

            //如果信噪比分数低于阈值，返回错误
            if (Double.valueOf(resultSnrScore) < Double.valueOf(snrScore)){
                return ResponseTemplate.error("snr分数过低，snr=" + resultSnrScore);
            }else {
               return ResponseTemplate.ok(verifyResultMap);
            }
            /*String getVerifyScore = (String) JSONObject.fromObject(verifyResultMap.get("result")).get("score");
            //符合验证得分小于阈值返回false，否则返回true
            if (Double.valueOf(getVerifyScore) > Double.valueOf(verifyScore)){
                return ResponseTemplate.ok(true);
            }else {
                return ResponseTemplate.ok(false);
            }*/
        }else {//返回错误结果
            return ResponseTemplate.error(verifyResult);
        }
    }

    private void waitThread() {
        while (getCompletedThreadCount() > 0) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    private List<String> accordingPathGetAllFilePath(String path){
        ContinueFTP2 ftp2 = new ContinueFTP2();
        try {
            ftp2.connect(ftpHostname, Integer.parseInt(ftpPort), ftpUserName,ftpPwd);

            return ftp2.getFileNameListFromFTP(path);
        } catch (IOException e) {
            logger.error("连接ftp异常：", e);
            return null;
        }
    }
}
