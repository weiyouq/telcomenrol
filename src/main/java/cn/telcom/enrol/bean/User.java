package cn.telcom.enrol.bean;

public class User {
    private Long id;

    private String userNo;

    private Integer enrolCategory;

    private String verifyNo;

    private Integer vpCount;

    public User(){

    }
    public User(String userNo, Integer enrolCategory, String verifyNo, Integer vpCount) {
        this.userNo = userNo;
        this.enrolCategory = enrolCategory;
        this.verifyNo = verifyNo;
        this.vpCount = vpCount;
    }

    public User(Long id, String verifyNo, Integer vpCount) {
        this.id = id;
        this.userNo = userNo;
        this.enrolCategory = enrolCategory;
        this.verifyNo = verifyNo;
        this.vpCount = vpCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo == null ? null : userNo.trim();
    }

    public Integer getEnrolCategory() {
        return enrolCategory;
    }

    public void setEnrolCategory(Integer enrolCategory) {
        this.enrolCategory = enrolCategory;
    }

    public String getVerifyNo() {
        return verifyNo;
    }

    public void setVerifyNo(String verifyNo) {
        this.verifyNo = verifyNo == null ? null : verifyNo.trim();
    }

    public Integer getVpCount() {
        return vpCount;
    }

    public void setVpCount(Integer vpCount) {
        this.vpCount = vpCount;
    }
}