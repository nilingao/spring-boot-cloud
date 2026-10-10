package cn.com.nla.common.video.basic.common;

/**
 * 视频流常量
 */
public class VideoConstant {

    /**
     * 缓存每次请求 对方的ip 端口
     */
    public static final String VIDEO_REQUEST_CALL_ID = "VIDEO_REQUEST_CALL_ID:";
    /**
     * 平台或这边注册的信息缓存（全局）
     */
    public static final String VIDEO_REGISTER_INFO = "VIDEO_REGISTER_INFO:";
    /**
     * 下载默认用户
     */
    public static final Long DEFAULT_DOWNLOAD_USER = 1000000000001010L;
    /**
     * Invite播放相关key 播放 回放 下载
     */
    public static final String INVITE_DOWNLOAD_USER_PREFIX = "VMP_INVITE_DOWNLOAD_USER";
    /**
     * 播流最大并发个数
     */
    public static final Integer MAX_STRTEAM_COUNT = 10000;
    /**
     * 允许事件延迟时长（秒）
     */
    public static final Integer DELAY_TIME = 5;
    /**
     * 视频流点播/回放缓存前缀Key
     */
    public static final String VIDEO_MEDIA_STREAM_CHANGED_PREFIX = "VIDEO_MEDIA_STREAM_CHANGED:";
    /**
     * 视频流点播/回放缓存前缀Key
     */
    public static final String MEDIA_TRANSACTION_USED_PREFIX = "VIDEO_MEDIA_TRANSACTION";
    /**
     *  流媒体SSRC缓存前戳 key
     */
    public static final String SSRC_CONFIG_INFO_PREFIX = "VIDEO_SSRC_CONFIG_INFO_PREFIX:";
    /**
     * 视频流推拉流缓存
     */
    public static final String PLATFORM_SEND_RTP_INFO_PREFIX = "VIDEO_PLATFORM_SEND_RTP_INFO";
    /**
     * 录像上传缓存
     */
    public static final String VIDEO_RECORD_MP4_INFO = "VIDEO_RECORD_MP4_INFO";
    /**
     * Invite播放相关key 播放 回放 下载
     */
    public static final String INVITE_PREFIX = "VMP_INVITE";

    /**
     * 设备注册缓存(注册流程中缓存的SIP服务相关信息)
     */
    public static final String DEVICE_PREFIX = "VIDEO_DEVICE_REGISTER:";
    /**
     * 上级平台注册缓存(注册流程中缓存的SIP服务相关信息)
     */
    public static final String PARENT_PLATFORM_PREFIX = "VIDEO_PARENT_PLATFORM_REGISTER:";
    /**
     * 设备过期任务
     */
    public static final String REGISTER_EXPIRE_TASK_KEY_PREFIX = "VIDEO_DEVICE_REGISTER_EXPIRE:";
    /**
     * 上级平台注册缓存(平台注册信息 用于二次认证找到对应平台)
     */
    public static final String PLATFORM_REGISTER_CATCH_PREFIX = "PLATFORM_REGISTER_CATCH:";
    /**
     * 上级平台注册缓存(平台注册信息 用于保活)
     */
    public static final String PLATFORM_KEEPALIVE_PREFIX = "PLATFORM_KEEPALIVE_PREFIX:";
    /**
     * 上级平台注册缓存（用于平台设置注册时间，到注册时间后续订）
     */
    public static final String PLATFORM_REGISTER_TASK_CATCH_PREFIX = "PLATFORM_REGISTER_TASK_PREFIX:";

    public static final String VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX = "VIDEO_MEDIA_ONLINE_SERVERS_COUNT:";
    /**
     * 全局SIP_CSEQ请求计数器
     */
    public static final String SIP_CSEQ_PREFIX = "VIDEO_SIP_CSEQ:";
    /**
     * 设备录像信息结果前缀
     */
    public static final String REDIS_RECORD_INFO_RES_PRE = "GB_RECORD_INFO_RES:";
    /**
     * Redis Const
     * 设备录像信息结果前缀
     */
    public static final String REDIS_RECORD_INFO_RES_COUNT_PRE = "GB_RECORD_INFO_RES_COUNT:";
    /**
     * 通过订阅同步设备通道信息
     */
    public static final String VIDEO_CATALOG_DATA_MANAGER = "VIDEO_CATALOG_DATA_MANAGER:";
    /**
     * 通过订阅同步设备通道信息
     */
    public static final String VIDEO_AUDIO_PUSH_MANAGER = "VIDEO_AUDIO_PUSH_MANAGER:";
    /**
     * 流媒体回调订阅事件key
     */
    public static final String MEDIA_HOOK_SUBSCRIBE_MANAGER = "MEDIA_HOOK_SUBSCRIBE_MANAGER";

    /**
     * 上级平台目录订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE = "VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE:";
    /**
     * 上级平台目录订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_CATALOG_NOTIFY_SUBSCRIBE = "VIDEO_PLATFORM_CATALOG_NOTIFY_SUBSCRIBE:";
    /**
     * 上级平台移动位置订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_MOBILE_POSITION_NOTIFY_SUBSCRIBE = "VIDEO_PLATFORM_MOBILE_POSITION_NOTIFY_SUBSCRIBE:";

    /**
     * 设备订阅缓存key
     */
    public static final String VIDEO_DEVICE_NOTIFY_SUBSCRIBE = "VIDEO_DEVICE_NOTIFY_SUBSCRIBE";
    /**
     * 设备报警订阅缓存key
     */
    public static final String VIDEO_DEVICE_ALARM_NOTIFY_SUBSCRIBE = "VIDEO_DEVICE_ALARM_SUBSCRIBE:";
    /**
     * 设备目录订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_CATALOG_NOTIFY_SUBSCRIBE = "VIDEO_DEVICE_CATALOG_NOTIFY_SUBSCRIBE:";
    /**
     * 设备移动位置订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_MOBILE_POSITION_NOTIFY_SUBSCRIBE = "VIDEO_DEVICE_MOBILE_POSITION_NOTIFY_SUBSCRIBE:";
    /**
     * 发送消息通知 （redis消息订阅key）
     */
    public static final String VIDEO_SEND_SIP_MESSAGE = "VIDEO_SEND_SIP_MESSAGE";
    /**
     * 发送平台注册消息
     */
    public static final String VIDEO_SEND_SIP_REGISTER_MESSAGE = "VIDEO_SEND_SIP_REGISTER_MESSAGE";
    /**
     * 平台自身注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_SIP_CACHE_SERVER = "VIDEO_REGISTER_CACHE_SERVER:SIP:";
    /**
     * 设备注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_CACHE_SERVER = "VIDEO_DEVICE_CACHE_SERVER:DEVICE:";
    /**
     * 上级注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_CACHE_SERVER = "VIDEO_PLATFORM_CACHE_SERVER:PLATFORM:";

    /**
     * Sip消息订阅key
     */
    public static final String VIDEO_SIP_EVENT_SUBSCRIBE_MANAGER = "VIDEO_SIP_EVENT_SUBSCRIBE_MANAGER";
    /**
     * Sip错误消息订阅key
     */
    public static final String VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER = "VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * Sip正确消息订阅key
     */
    public static final String VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER = "VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER:";
    /**
     * 录像结束消息订阅key
     */
    public static final String VIDEO_RECORD_END_SUBSCRIBE_MANAGER = "VIDEO_RECORD_END_SUBSCRIBE_MANAGER:";

    /**
     * 等待回复回调key
     */
    public static final String VIDEO_DEFERRED_RESULT_HOLDER = "VIDEO_DEFERRED_RESULT_HOLDER:";


}
