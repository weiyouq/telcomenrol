package cn.telcom.enrol.bean;

public class UserBusiness {
    private Long id;

    private Long userId;

    private Integer buId;

    public UserBusiness(Long userId, Integer buId) {
        this.userId = userId;
        this.buId = buId;
    }

    public UserBusiness() {
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

    public Integer getBuId() {
        return buId;
    }

    public void setBuId(Integer buId) {
        this.buId = buId;
    }
}