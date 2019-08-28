package cn.telcom.enrol.dao;

import cn.telcom.enrol.bean.Business;

/**
 * @author kenny_peng
 * @created 2019/8/2 15:32
 */
public interface IBusinessDao extends IBaseDao<Integer, Business> {
    Business selectByBuNo(String buNo);
}
