package cn.com.nla.common.freeswitch.common.sip;

/**
 * 信令服务相关参数
 */
public class SipConstant {

    /** 十六进制转换器 */
    public static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static Integer maxForwardsHeader = 70;
    /**
     * 允许事件延迟时长（秒）
     */
    public static final Integer DELAY_TIME = 3;
    /**
     * 信令默认密码认证方式
     */
    public static final String DEFAULT_ALGORITHM = "MD5";
    /**
     * 信令默认认证头部
     */
    public static final String DEFAULT_SCHEME = "Digest";

    /**
     * 设备注册缓存(注册流程中缓存的SIP服务相关信息)
     */
    public static final String DEVICE_PREFIX = "FS_DEVICE_REGISTER:";
    /**
     * 上级平台注册缓存(注册流程中缓存的SIP服务相关信息)
     */
    public static final String PARENT_PLATFORM_PREFIX = "FS_PARENT_PLATFORM_REGISTER:";
    /**
     * 平台自身注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_SIP_CACHE_SERVER = "FS_REGISTER_CACHE_SERVER:SIP:";
    /**
     * 设备注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_CACHE_SERVER = "FS_DEVICE_CACHE_SERVER:DEVICE:";
    /**
     * 上级注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_CACHE_SERVER = "FS_PLATFORM_CACHE_SERVER:PLATFORM:";
    /**
     * 发送消息通知 （redis消息订阅key）
     */
    public static final String VIDEO_SEND_SIP_MESSAGE = "FS_SEND_SIP_MESSAGE";
    /**
     * Sip消息订阅key
     */
    public static final String VIDEO_SIP_EVENT_SUBSCRIBE_MANAGER = "FS_SIP_EVENT_SUBSCRIBE_MANAGER";
    /**
     * Sip错误消息订阅key
     */
    public static final String VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER = "FS_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * Sip正确消息订阅key
     */
    public static final String VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER = "FS_SIP_OK_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * Sip消息订阅key
     */
    public static final String VIDEO_AGENT_EVENT_SUBSCRIBE_MANAGER = "FS_AGENT_EVENT_SUBSCRIBE_MANAGER";
    /**
     * Sip错误消息订阅key
     */
    public static final String VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER = "FS_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * Sip正确消息订阅key
     */
    public static final String VIDEO_AGENT_OK_EVENT_SUBSCRIBE_MANAGER = "FS_AGENT_OK_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * Sip消息订阅key
     */
    public static final String FS_AGENT_LOGIN_SUBSCRIBE_MANAGER = "FS_AGENT_LOGIN_SUBSCRIBE_MANAGER:";
    /**
     * Sip正确消息订阅key
     */
    public static final String FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER = "FS_AGENT_LOGIN_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * 上级平台注册缓存(平台注册信息 用于二次认证找到对应平台)
     */
    public static final String PLATFORM_REGISTER_CATCH_PREFIX = "FS_PLATFORM_REGISTER_CATCH:";
    /**
     * 上级平台注册缓存(平台注册信息 用于保活)
     */
    public static final String PLATFORM_KEEPALIVE_PREFIX = "FS_PLATFORM_KEEPALIVE_PREFIX:";
    /**
     * 全局SIP_CSEQ请求计数器
     */
    public static final String SIP_CSEQ_PREFIX = "FS_SIP_CSEQ:";
    /**
     * 上级平台注册缓存（用于平台设置注册时间，到注册时间后续订）
     */
    public static final String PLATFORM_REGISTER_TASK_CATCH_PREFIX = "FS_PLATFORM_REGISTER_TASK_PREFIX:";
    /**
     * 视频流推拉流缓存
     */
    public static final String PLATFORM_SEND_RTP_INFO_PREFIX = "FS_PLATFORM_SEND_RTP_INFO";
    /**
     *  流媒体SSRC缓存前戳 key
     */
    public static final String SSRC_CONFIG_INFO_PREFIX = "FS_SSRC_CONFIG_INFO_PREFIX:";
    /**
     * 流媒体回调订阅事件key
     */
    public static final String MEDIA_HOOK_SUBSCRIBE_MANAGER = "FS_MEDIA_HOOK_SUBSCRIBE_MANAGER";
    public static final String VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX = "FS_MEDIA_ONLINE_SERVERS_COUNT:";
    /**
     * 视频流点播/回放缓存前缀Key
     */
    public static final String VIDEO_MEDIA_STREAM_CHANGED_PREFIX = "FS_MEDIA_STREAM_CHANGED:";
    /**
     * Invite播放相关key 播放 回放 下载
     */
    public static final String INVITE_PREFIX = "FS_VMP_INVITE";
    /**
     * Invite播放相关key 播放 回放 下载
     */
    public static final String INVITE_DOWNLOAD_USER_PREFIX = "FS_VMP_INVITE_DOWNLOAD_USER";
    /**
     * 录像上传缓存
     */
    public static final String VIDEO_RECORD_MP4_INFO = "FS_RECORD_MP4_INFO";
    /**
     * 视频流点播/回放缓存前缀Key
     */
    public static final String MEDIA_TRANSACTION_USED_PREFIX = "FS_MEDIA_TRANSACTION";
    /**
     * 等待回复回调key
     */
    public static final String VIDEO_DEFERRED_RESULT_HOLDER = "FS_DEFERRED_RESULT_HOLDER:";
    /**
     * 发送平台注册消息
     */
    public static final String VIDEO_SEND_SIP_REGISTER_MESSAGE = "FS_SEND_SIP_REGISTER_MESSAGE";
    /**
     * 设备过期任务
     */
    public static final String REGISTER_EXPIRE_TASK_KEY_PREFIX = "FS_DEVICE_REGISTER_EXPIRE:";

}
