package cn.telcom.enrol.config.response;

import java.util.HashMap;

/**
 * @author kenny_peng
 * @created 2019/7/25 10:44
 */
public class ResponseTemplate extends HashMap<String, Object> {

    private static final Integer SUCCESS = 0;
    private static final Integer Failed = 500;

    public ResponseTemplate(){
        put("code",SUCCESS);
        put("msg","操作成功");
    }


    public static ResponseTemplate ok(){
        return new ResponseTemplate();
    }

    public static ResponseTemplate ok(Object o){
        ResponseTemplate responseTemplate = new ResponseTemplate();
        responseTemplate.put("code", SUCCESS);
        responseTemplate.put("msg", o);
        return responseTemplate;
    }

    public static ResponseTemplate error(){
        return ResponseTemplate.error("操作失败");
    }

    public static ResponseTemplate error(Object o){
        ResponseTemplate rst = new ResponseTemplate();
        rst.put("code", Failed);
        rst.put("msg", o);
        return rst;
    }

    @Override
    public ResponseTemplate put(String key, Object value){
        super.put(key, value);
        return this;
    }

}
