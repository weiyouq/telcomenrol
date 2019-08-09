package cn.telcom.enrol.service;


import cn.telcom.enrol.bean.ReceiveVoiceInputObject;
import cn.telcom.enrol.config.response.ResponseTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author kenny_peng
 * @created 2019/7/25 10:41
 */
public interface IEnrolService {

    /**
     * 1对1声纹注册
     * @return
     */
    @Transactional
    String toEnrolSpeaker();

    /**
     * 1对多声纹注册
     * @param path  选择的路径
     * @return
     */
    @Transactional
    String identifyService(String path);
}
