package cn.telcom.enrol.service.impl;


import cn.telcom.enrol.Utils.AudioUtils;
import cn.telcom.enrol.Utils.FileUtils;
import cn.telcom.enrol.Utils.PayloadUtils;
import cn.telcom.enrol.Utils.PostRequest;
import cn.telcom.enrol.bean.*;
import cn.telcom.enrol.config.response.ResponseTemplate;
import cn.telcom.enrol.dao.*;
import cn.telcom.enrol.service.IEnrolService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.collections.bag.SynchronizedSortedBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
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

    private List<File> needToVBEnrolFiles = new ArrayList<>();//一对一需要注册的音频
    private List<File> needToIdentifyEnrolFiles = new ArrayList<>();//一对多需要注册的音频

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
        String nowDayFilePath = voiceSaveLocation + File.separator + FileUtils.formateDate("yyyyMMdd") + File.separator;
        File file = new File(nowDayFilePath);
        List<File> listFiles = FileUtils.getFileList(file);
        logger.info("查询文件路径nowDayFilePath：" + nowDayFilePath + "------" + "得到的文件集合大小listFiles.size()：" + listFiles.size());

        List<String> alreadyEnroledList = activityLogDao.selectAlreadyEnroledList();
        List<ResponseTemplate> resultList = new ArrayList<>();
        for (File singleFile : listFiles){
            if (!alreadyEnroledList.contains(singleFile.getPath())){
                needToVBEnrolFiles.add(singleFile);
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
        File file = null;
        while ((file=popAudioItem(0)) != null) {
            ResponseTemplate responseTemplate = vbEnrolProcessMethod(file);
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
        File file = null;
        while ((file=popAudioItem(1)) != null) {
            //进行注册
            String enrolResult = identifyEnrolSpeaker(file.getPath());
            Map<String, Object> map = JSONObject.fromObject(enrolResult);

            //如果包含retCode，注册成功，否则、注册失败
            if (map.containsKey("retCode")){
                success.add(file.getPath());
            }else{
                error.add(file.getPath() + enrolResult);
            }
        }
        setCompletedThreadCount(-1);//结束循环用
    }

    /**
     * 获取需要注册的file对象
     * @return
     */
    private synchronized File popAudioItem(int taskName) {
        if (taskName == 0 ){
            if (needToVBEnrolFiles.size() == 0)
                return null;
            File item = needToVBEnrolFiles.get(0);
            needToVBEnrolFiles.remove(0);
            return item;
        }else {
            if (needToIdentifyEnrolFiles.size() == 0)
                return null;
            File item = needToIdentifyEnrolFiles.get(0);
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
        File file = new File(filePath);
//        if (file.isDirectory()){
        if (!file.isFile()){
            needToIdentifyEnrolFiles.clear();
            //如果filePath是一个目录，遍历得到所有文件
            needToIdentifyEnrolFiles = FileUtils.getFileList(file);
            logger.info("查询文件路径filePath：" + filePath + "--------" + "得到的文件集合大小needToIdentifyEnrolFiles.size()：" + needToIdentifyEnrolFiles.size());

            if (needToIdentifyEnrolFiles.size() == 0){
                return JSONObject.fromObject(ResponseTemplate.error("选择目录为空，请重新选择!")).toString();
            }else {
                Map<String, List<String>> map = new HashMap<>();
                Object start = start(map, 1);
                return JSONObject.fromObject(start).toString();
            }
        }else {
            //如果file,则针对该文件进行注册
            String enrolSpeaker = identifyEnrolSpeaker(filePath);
            return enrolSpeaker;
        }
    }

    private String identifyEnrolSpeaker(String filePath) {
        String enrolBase64 = AudioUtils.audioToBase64(filePath);
        String fileName = new File(filePath).getName();
        String[] names = getUserArray(fileName);
        if (names != null){

            //1、先进性1对1验证，判断性噪比


            String enrolPayload = PayloadUtils.identifyPayload(names[0], enrolBase64);
            String enrolResult = PostRequest.sendPost(getUseUrl(identifyEnrolAddress), enrolPayload);
            User user = userDao.selectUserByNoAndEnrolCategory(names[0], 1);
            Business business = businessDao.selectByBuNo(names[1]);
            int buID = business.getId();
            long userID;//= user.getId();
            if (user == null){
                User newUser = new User(names[0],1,null,null);
                userDao.insertSelective(newUser);
                userID = newUser.getId();
                if (business == null){
                    Business newBusiness = new Business(names[1]);
                    businessDao.insertSelective(newBusiness);
                    buID = newBusiness.getId();
                }
            }else {
                userID = user.getId();
            }
            UserBusiness userBusiness = userBusinessDao.selectUserByUserIdAndBuId(userID, buID);
            if (userBusiness == null){
                userBusinessDao.insertSelective(new UserBusiness(userID,buID));
            }
            if (enrolResult.length() > 200){
                activityLogDao.insertSelective(new ActivityLog(userID,buID,filePath,1001,new Date(),enrolResult.substring(0,200)));
            }else {
                activityLogDao.insertSelective(new ActivityLog(userID,buID,filePath,1001,new Date(),enrolResult));
            }
            return enrolResult;
        }else{
            return JSONObject.fromObject(ResponseTemplate.error("注册音频命名不规范，请按照要求命名")).toString();
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
     * @param file          文件对象
     * @return
     */
    private ResponseTemplate vbEnrolProcessMethod(File file) {
        String base64 = AudioUtils.audioToBase64(file.getPath());
        String logName = "log-"+ FileUtils.formateDate("yyyyMMddHHmmssSSS");//log日志名
        String fileName = file.getName();
        String userNo = fileName.substring(0,fileName.indexOf("_"));
        String bu_no = fileName.substring(fileName.indexOf("_")+1, fileName.lastIndexOf("."));

        //1、查询用户是存在
        User user = userDao.selectUserByNoAndEnrolCategory(userNo, 0);
        Business business = businessDao.selectByBuNo(bu_no);
        if(user == null){
            //1、声纹预备注册
            String userNo_prepare = userNo + "_2";
            ResponseTemplate responseTemplate = enrolSpeaker(logName, userNo_prepare, base64);
            User newUser = new User();
            newUser.setUserNo(userNo);
            newUser.setEnrolCategory(0);
            newUser.setVpCount(1);
            userDao.insertSelective(newUser);

            int buId;
            if (business == null){
                Business newBusiness = new Business();
                newBusiness.setBuNo(bu_no);
                businessDao.insertSelective(newBusiness);
                buId = newBusiness.getId();
            }else {
                buId = business.getId();
            }
            userBusinessDao.insertSelective(new UserBusiness(newUser.getId(), buId));
            activityLogDao.insertSelective(new ActivityLog(newUser.getId(), buId, file.getPath(),1, new Date(),JSONObject.fromObject(responseTemplate).toString()));
            return ResponseTemplate.ok("注册成功");
        }else {
            if (business == null){
                business = new Business(bu_no);
                businessDao.insertSelective(business);
            }
            userBusinessDao.selectUserByUserIdAndBuId(user.getId(),business.getId());
            if (userBusinessDao == null){
                userBusinessDao.insertSelective(new UserBusiness(user.getId(),business.getId()));
            }

            //判断用户注册数量
            if (user.getVpCount() == 1){
                if (user.getVerifyNo() == "" || user.getVerifyNo() == null){//说明有的一条模型是预注册
                    //1、跟预注册声纹模型进行验证
                    ResponseTemplate responseTemplate = verifySpeaker(logName, userNo + "_2", base64);
                    if ((int)responseTemplate.get("code") == 0){
                        if ((boolean)responseTemplate.get("msg") == true){//验证通过
                            userDao.updateByPrimaryKeySelective(new User(user.getId(),userNo + "_2",1));
                            String res = JSONObject.fromObject(responseTemplate).toString();
                            if (res.length() > 200){
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),res.substring(0,200)));

                            }else {
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),res));

                            }
                            return responseTemplate;
                        }else {
                            //1、声纹注册（预注册）
                            ResponseTemplate responseTemplate1 = enrolSpeaker(logName, userNo + "_2", base64);
                            String ss1 = JSONObject.fromObject(responseTemplate1).toString();
                            if (ss1.length() > 200){
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(), ss1.substring(0,200)));
                            }else {
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(), ss1));

                            }
                            return responseTemplate1;
                        }
                    }else{
                        return ResponseTemplate.error(file.getPath() + "注册失败"+ responseTemplate);
                    }
                }else {//user.getVerify_no（）不为空，说明有的一条模型是激活的模型
                    //预注册模型的用户编号
                    String prepareEnrolNo = getPrepareEnrolNo(user.getVerifyNo());
                    //1、跟激活的模型进行验证
                    ResponseTemplate responseTemplate = verifySpeaker(logName, user.getVerifyNo(), base64);
                    if ((int)responseTemplate.get("code") == 0){
                        if ((boolean)responseTemplate.get("msg") == true) {//验证通过
                            //1、添加验证记录
                            if (JSONObject.fromObject(responseTemplate).toString().length() > 200){
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString().substring(0,200)));
                            }else {
                                activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString()));
                            }

                            return ResponseTemplate.ok(file.getPath() + "注册成功");
                        }else {//验证不通过

                            //1、声纹注册（作为预注册模型）
                            ResponseTemplate responseTemplate1 = enrolSpeaker(logName, prepareEnrolNo, base64);
                            //2、更新用户表t_user set vp_count=2
                            userDao.updateByPrimaryKeySelective(new User(user.getId(), null,2));
                            List<ActivityLog> listActivityLog = new ArrayList<>();
                            if (JSONObject.fromObject(responseTemplate1).toString().length() > 200){
                                listActivityLog.add(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(),JSONObject.fromObject(responseTemplate1).toString().substring(0,200)));
                                listActivityLog.add(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString().substring(0,200)));
                            }else {
                                listActivityLog.add(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(),JSONObject.fromObject(responseTemplate1).toString()));
                                listActivityLog.add(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString()));
                            }
                            activityLogDao.insertByList(listActivityLog);
                            return responseTemplate1;
                        }
                    }else {
                        return ResponseTemplate.error(file.getPath() + "注册失败"+ responseTemplate);
                    }
                }
            }else {//有两条模型,说明激活的模型，和预注册的模型都有，肯定有user.getVerify_no(),
                //1、跟激活的模型进行验证
                ResponseTemplate responseTemplate = verifySpeaker(logName, user.getVerifyNo(), base64);
                if ((int)responseTemplate.get("code") == 0){
                    if ((boolean)responseTemplate.get("msg") == true) {//验证通过
//                        //预注册模型的用户编号
//                        String prepareEnrolNo = getPrepareEnrolNo(user.getVerifyNo());
                        //1、add验证记录表
                        if (JSONObject.fromObject(responseTemplate).toString().length() > 200){
                            activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString().substring(0,200)));
                        }else {
                            activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate).toString().substring(0,200)));
                        }
                        return ResponseTemplate.ok(file.getPath() + "注册成功");
                    }else {
                        //预注册模型的用户编号
                        String prepareEnrolNo = getPrepareEnrolNo(user.getVerifyNo());
                        //1、跟预注册声纹模型进行验证
                        ResponseTemplate responseTemplate2 = verifySpeaker(logName, prepareEnrolNo, base64);
                        if ((int)responseTemplate2.get("code") == 0){
                            if ((boolean)responseTemplate2.get("msg") == true) {//验证通过
                                userDao.updateByPrimaryKeySelective(new User(user.getId(), prepareEnrolNo,1));
                                if (JSONObject.fromObject(responseTemplate2).toString().length() > 200){
                                    activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate2).toString().substring(0,200)));
                                }else {
                                    activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 3, new Date(),JSONObject.fromObject(responseTemplate2).toString()));

                                }
                                return ResponseTemplate.ok(file.getPath() + "注册成功");
                            }else {
                                //1、声纹注册（预注册）
                                ResponseTemplate responseTemplate1 = enrolSpeaker(logName, prepareEnrolNo, base64);
                                String res = JSONObject.fromObject(responseTemplate1).toString();
                                if (res.length() > 200){
                                    activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(),res.substring(0,200)));
                                }else {
                                    activityLogDao.insertSelective(new ActivityLog(user.getId(), business.getId(), file.getPath(), 1, new Date(),res));
                                }
                                return ResponseTemplate.ok(file.getPath() + "注册成功");
                            }
                        }else {
                            return ResponseTemplate.error(file.getPath() + "注册失败"+ responseTemplate2);
                        }
                    }
                }else {
                    return ResponseTemplate.error(file.getPath() + "注册失败"+ responseTemplate);
                }
            }

        }
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

//            //解析verifyResult，得到信噪比值
//            Object o = JSONObject.fromObject(verifyResultMap.get("result")).get("metaInformation");
//            JSONArray jsonArray = JSONArray.fromObject(o);
//            String resultSnrScore = null;
//            for (int i = 0; i<jsonArray.size(); i++){
//                if (JSONObject.fromObject(jsonArray.getString(i)).get("key").equals("get-snr")){
//                    resultSnrScore = (String) JSONObject.fromObject(JSONObject.fromObject(jsonArray.getString(i)).get("value")).get("value");
//                }
//            }

//            //如果信噪比分数低于阈值，返回错误
//            if (Double.valueOf(resultSnrScore) < Double.valueOf(snrScore)){
//                return ResponseTemplate.error("分数低于阈值");
//            }else {
//                String getVerifyScore = (String) JSONObject.fromObject(verifyResultMap.get("result")).get("score");
//
//                //符合验证得分小于阈值返回false，否则返回true
//                if (Double.valueOf(getVerifyScore) > Double.valueOf(verifyScore)){
//                    return ResponseTemplate.ok(true);
//                }else {
//                    return ResponseTemplate.ok(false);
//                }
//            }
            String getVerifyScore = (String) JSONObject.fromObject(verifyResultMap.get("result")).get("score");
            //符合验证得分小于阈值返回false，否则返回true
            if (Double.valueOf(getVerifyScore) > Double.valueOf(verifyScore)){
                return ResponseTemplate.ok(true);
            }else {
                return ResponseTemplate.ok(false);
            }
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


}
