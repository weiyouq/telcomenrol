package cn.telcom.enrol.dao;

/**
 * 增删改查
 *
 * @author kenny_peng
 * @created 2019/7/25 12:26
 */
public interface IBaseDao<K, T> {

    /**
     * 根据主键删除
     *
     * @param id 主键
     * @return 受影响行数
     */
    int deleteByPrimaryKey(K id);

    /**
     * 插入一条数据
     *
     * @param t 插入实体类
     * @return 受影响行数
     */
    int insert(T t);

    /**
     * 插入一条数据，插入时判断字段是否为空
     *
     * @param t 插入实体类
     * @return 受影响行数
     */
    int insertSelective(T t);

    /**
     * 根据传入实体类更新数据
     *
     * @param t 实体类
     * @return 受影响行数
     */
    int updateByPrimaryKeySelective(T t);

    /**
     * 根据传入实体类更新，更新时判断字段是否为空
     *
     * @param t 实体类
     * @return 受影响行数
     */
    int updateByPrimaryKey(T t);

    /**
     * 根据主键查找
     *
     * @param k 主键
     * @return 实体类
     */
    T selectByPrimaryKey(K k);
}
