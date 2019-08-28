package cn.telcom.enrol.config;

/**
 * @author kenny_peng
 * @created 2019/8/28 11:18
 */
public class Const {


    //1对1文件下载失败
    public  static  final int DOWNLOAD_FAILED = 0;

    //1、1对1注册，
    public static final int VB_ENROL = 1;
    // 2、1对1加强，
    public static final int VB_UPDATE = 2;
    // 3、1对1验证，
    public static final int VB_VERIFY = 3;
    // 4、1对1删除，
    public static final int VB_DELETE = 4;
    // 5、1对1查询是否存在，
    public static final int VB_IS_EXIST = 5;

    // 10、1对1注册失败，
    public static final int VB_ENROL_FAILED = 10;
    // 20、1对1加强失败，
    public static final int VB_UPDATE_FAILED = 20;
    // 30、1对1验证失败，
    public static final int VB_VERIFY_FAILED = 30;
    // 40、1对1删除失败，
    public static final int VB_DELETE_FAILED = 40;
    // 50、1对1查询是否存在失败；
    public static final int VB_IS_EXIST_FILED = 50;

    //今天已经注册过，不在进行注册
    public static final int VB_NOW_DAY_ENROLED = 100;


    // 1001、1对多注册，
    public static final int IDENTIFY_ENROL = 1001;
    // 1002、1对多强化，
    public static final int IDENTIFY_UPDATE = 1002;
    // 1003、1对多的一对一验证，
    public static final int IDENTIFY_VERIFY_ONE = 1003;
    // 1004、1对多根据用户前缀验证，
    public static final int IDENTIFY_BY_PREFIX = 1004;
    // 1005、1对多的一对多验证，
    public static final int IDENTIFY_VERIFY_MORE = 1005;
    // 1006、1对N删除账户，
    public static final int IDENTIFY_DELETE = 1006;
    // 1007、1对多查询用户是否存在，
    public static final int IDENTIFY_IS_EXIST = 1007;


    // 10010、1对多注册失败，
    public static final int IDENTIFY_ENROL_FAILED = 10010;
    // 10020、1对多强化失败，
    public static final int IDENTIFY_UPDATE_FAILED = 10020;
    // 10030、1对多的一对一验证失败，
    public static final int IDENTIFY_VERIFY_ONE_FAILED = 10030;
    // 10040、1对多根据用户前缀验证失败，
    public static final int IDENTIFY_BY_PREFIX_FAILED = 10040;
    // 10050、1对多的一对多验证失败，
    public static final int IDENTIFY_VERIFY_MORE_FAILED = 10050;
    // 10060、1对N删除账户失败，
    public static final int IDENTIFY_DELETE_FAILED = 10060;
    // 10070、1对多查询用户是否存在失败
    public static final int IDENTIFY_IS_EXIST_FAILED = 10070;


    //1对多文件下载失败
    public  static final int IDENTIFY_DOWNLOAD_FAILED = 10000;


    
}
