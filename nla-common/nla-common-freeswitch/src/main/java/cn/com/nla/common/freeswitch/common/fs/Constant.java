package cn.com.nla.common.freeswitch.common.fs;

public class Constant {
    /**
     * 统一日期时间格式
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String AGENT_SIP_PREFIX = "01";//客服SIP号码前缀
    public static final String CONFERENCE_ID_PREFIX = "02";//会议号码前缀
    public static final String AT = "@";
    public static final String CO = ":";
    public static final String UNDER_LINE = "_";
    public static final String POINT = ".";
    public final static String SPLIT = ",";
    public static final String SK = "/";
    public static final String LINE = "-";
    public static final String JSON = ".json";
    public static final String XML = ".xml";
    //自定义通道变量（重要作用）
    public final static String SIP_HEADER = "sip_h_X-";
    public final static String EQ = "=";
    public final static String SET = "set";
    public final static String OK = "OK";
    //挂断命令
    public final static String HANGUP = "hangup";
    public final static String NORMAL_CLEARING = "NORMAL_CLEARING";
    public final static String SOFIA = "sofia";
    // 挂起，等待接听
    public final static String PARK = " &park()";
    // 挂起，并播放默认音乐
    public final static String HOLD = " &hold()";
    // 挂起，并播放一个特定声音，需指定声音文件路径
    public final static String PLAYBACK = "playback";
    //发起一个呼叫
    public final static String ORIGINATE = "originate";
    public final static String EXECUTE = "execute";
    public final static String ANSWER = "answer";
    // 如果设置为true，它将在桥接返回后驻留呼叫。 在transfer_after_bridge和hangup_after_bridge之前进行检查。
    public final static String PARK_AFTER_BRIDGE = "park_after_bridge=true";
    // 语音文件地址
    public final static String SOUND_PREFIX = "/usr/share/freeswitch/sounds/zh/cn/link";
    //  控制处于桥接状态且被叫方（B）挂断时主叫方（A）发生的情况。如果true拨号计划将停止处理，并且A分支将在B分支终止时终止。如果false（默认）B腿终止后，拨号计划将继续处理。
    public final static String HANGUP_AFTER_BRIDGE = "hangup_after_bridge=false";
    public final static String UUID_KILL = "uuid_kill";
    public final static String UUID_PHONE_EVENT = "uuid_phone_event";
    public final static String PLAYBACK_TERMINATORS = "playback_terminators=none";
    public final static String PLAYBACK_DELIMITER = "playback_delimiter=!";
    public final static String TTS_ENGINE = "tts_engine=unimrcp";
    //关闭录音
    public final static String BREAK = "break";
    public final static String RECORD_SAMPLE_RATE = "record_sample_rate=";
    public final static String RECORD_STEREO = " RECORD_STEREO true";
    public final static String START = "start";
    /**
     * 指定坐席
     */
    public static final String DESIGNATE_AGENT = "desiganteAgent";
    /**
     * vip等级进线
     */
    public static final String VIP_LEVEL = "vipLevel";
    public static final String START_TIME = "startTime";
    public static final String QUEUE_TIME = "queueTime";
    /**
     * 当前空闲时间
     */
    public static final String LONG_READY = "longReady";

    /**
     * 空闲次数
     */
    public static final String READY_TIMES = "readyTimes";

    /**
     * 服务次数
     */
    public static final String SEREVICE_TIMES = "serviceTimes";

}
