package cn.telcom.enrol.controller;

import cn.telcom.enrol.Utils.AudioUtils;
import cn.telcom.enrol.Utils.ShellExcutor;
import cn.telcom.enrol.service.IEnrolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author kenny_peng
 * @created 2019/7/25 10:09
 */
@Controller
@Scope("prototype")
public class TelcomEnrolController {

    @Autowired
    private IEnrolService iEnrolService;
    private Logger logger = LoggerFactory.getLogger(TelcomEnrolController.class);

    /**
     * 1对1注册接口
     * @return
     */
    @RequestMapping("/stdBiometricLite/enrolSpeaker")
    @ResponseBody
    public String toEnrolSpeaker(){
        return iEnrolService.toEnrolSpeaker();
    }


    /**
     * 1对多注册接口
     * @param path      选择的音频路径
     * @return
     */
    @PostMapping("/biometric/enrolSpeaker")
    @ResponseBody
    public String identifyService(@RequestBody String path, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return iEnrolService.identifyService(path);
    }

    @RequestMapping("/testhello")
    @ResponseBody
    public String test() throws Exception {
        new ShellExcutor().callScript("/root/Desktop/start.sh",null);
        return "123";
    }

    @RequestMapping("/audiotobase64")
    @ResponseBody
    public String base64(@RequestBody String path,HttpServletRequest request, HttpServletResponse response) throws IOException {
        String base64 = AudioUtils.audioToBase64(path);
        String base64Path = path.substring(0, path.lastIndexOf(".")) + ".txt";
        FileWriter fileWriter = new FileWriter(base64Path);
        fileWriter.write(base64);
        return "success";
    }


}
