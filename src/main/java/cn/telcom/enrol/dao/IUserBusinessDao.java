package cn.telcom.enrol.dao;

import cn.telcom.enrol.bean.UserBusiness;

/**
 * @author kenny_peng
 * @created 2019/8/2 15:22
 */
public interface IUserBusinessDao extends IBaseDao<Long, UserBusiness> {
    UserBusiness selectUserByUserIdAndBuId(Long userID, Integer buID);
}
