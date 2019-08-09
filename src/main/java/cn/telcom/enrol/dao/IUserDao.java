package cn.telcom.enrol.dao;

import cn.telcom.enrol.bean.User;

/**
 * @author kenny_peng
 * @created 2019/8/2 15:13
 */
public interface IUserDao extends IBaseDao<Long, User> {

    User selectUserByNoAndEnrolCategory(String userNo, int enrolCategory);
}
