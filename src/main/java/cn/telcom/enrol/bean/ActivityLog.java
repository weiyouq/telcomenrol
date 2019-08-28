package cn.telcom.enrol.bean;

import java.util.Date;

public class ActivityLog {
    private Long id;

    private Long userId;

    private int buId;

    private String voiceLocation;

    private Integer category;

    private Date createDate;

    private String result;

    public ActivityLog(Long userId, Integer buId, String voiceLocation, Integer category, Date createDate, String result) {

        this.userId = userId;
        if (buId != null){
            this.buId = buId;
        }

        this.voiceLocation = voiceLocation;
        this.category = category;
        this.createDate = createDate;
        this.result = result;
    }

    public ActivityLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getBuId() {
        return buId;
    }

    public void setBuId(int buId) {
        this.buId = buId;
    }

    public String getVoiceLocation() {
        return voiceLocation;
    }

    public void setVoiceLocation(String voiceLocation) {
        this.voiceLocation = voiceLocation == null ? null : voiceLocation.trim();
    }

    public Integer getCategory() {
        return category;
    }

    public void setCategory(Integer category) {
        this.category = category;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result == null ? null : result.trim();
    }
}