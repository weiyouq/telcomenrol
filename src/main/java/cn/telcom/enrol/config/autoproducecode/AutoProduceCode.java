package cn.telcom.enrol.config.autoproducecode;

import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.config.Configuration;
import org.mybatis.generator.config.xml.ConfigurationParser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动生成mybatis、bean代码
 * @author kenny_peng
 * @created 2019/7/1713:43
 */
public class AutoProduceCode {

    public static void main(String[] args) {
        new AutoProduceCode().test();
    }

    private void test(){
        List<String> warnings = new ArrayList<>();
        Boolean ovrWrite = true;
        URL configUrl = ClassLoader.getSystemResource("GeneratorConfig.xml");
        File file = new File(configUrl.getFile());
        ConfigurationParser cp = new ConfigurationParser(warnings);
        try {
            Configuration config = cp.parseConfiguration(file);
            ShellCallback back = new ShellCallbackEx(ovrWrite);
            MyBatisGenerator my = new MyBatisGenerator(config, back, warnings);
            my.generate(null);
            for (String item : warnings) {
                System.out.println(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("ok!!");
    }


}
