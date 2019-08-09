package cn.telcom.enrol.bean;

public class Business {
    private Integer id;

    private String buNo;

    public Business(String buNo) {
        this.buNo = buNo;
    }

    public Business() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBuNo() {
        return buNo;
    }

    public void setBuNo(String buNo) {
        this.buNo = buNo == null ? null : buNo.trim();
    }
}