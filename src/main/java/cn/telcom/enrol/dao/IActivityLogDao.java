package cn.telcom.enrol.dao;

import cn.telcom.enrol.bean.ActivityLog;

import java.util.List;

/**
 * @author kenny_peng
 * @created 2019/8/1 11:41
 */
public interface IActivityLogDao extends IBaseDao<Long, ActivityLog>{


    /**
     * 已经注册过的集合
     * @return
     */
    List<String> selectAlreadyEnroledList();

    int insertByList(List<ActivityLog> list);

    int insertByUserNoAndBuNo(String name, String name1);
}
