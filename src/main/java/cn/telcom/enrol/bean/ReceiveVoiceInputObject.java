package cn.telcom.enrol.bean;

/**
 * 保存音频时，传入参数定义
 * @author kenny_peng
 * @created 2019/7/25 10:22
 */
public class ReceiveVoiceInputObject {

    private String userNo;
    private String whichDepartmentUse;
    private String base64;

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getWhichDepartmentUse() {
        return whichDepartmentUse;
    }

    public void setWhichDepartmentUse(String whichDepartmentUse) {
        this.whichDepartmentUse = whichDepartmentUse;
    }

    public String getBase64() {
        return base64;
    }

    public void setBase64(String base64) {
        this.base64 = base64;
    }
}
