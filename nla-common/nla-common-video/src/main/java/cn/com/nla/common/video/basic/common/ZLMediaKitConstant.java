package cn.com.nla.common.video.basic.common;

public class ZLMediaKitConstant {

    /**
     * 流媒体服务地址
     */
    public static final String MEDIA_BASEURL = "baseUrl";
    /**
     * 流媒体服务秘钥
     */
    public static final String MEDIA_SECRET = "secret";
    /**
     * 流媒体请求前缀
     */
    private static final String URL_PREFIX = "/index/api/";
    /**
     * 获取流媒体服务器api列表
     */
    public static final String GET_API_LIST = URL_PREFIX + "getApiList";
    /**
     * 获取网络线程负载
     */
    public static final String GET_THREADS_LOAD = URL_PREFIX + "getThreadsLoad";
    /**
     * 获取后台线程负载
     */
    public static final String GET_WORK_THREADS_LOAD = URL_PREFIX + "getWorkThreadsLoad";
    /**
     * 获取服务器配置
     */
    public static final String GET_SERVER_CONFIG = URL_PREFIX + "getServerConfig";
    /**
     * 设置服务器配置
     * 参数 ： map
     */
    public static final String SET_SERVER_CONFIG = URL_PREFIX + "setServerConfig";
    /**
     * 重启服务器
     */
    public static final String RESTART_SERVER = URL_PREFIX + "restartServer";
    /**
     * 获取流列表
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String GET_MEDIA_LIST = URL_PREFIX + "getMediaList";
    /**
     * 关断单个流
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String CLOSE_STREAM = URL_PREFIX + "close_stream";
    /**
     * 批量关断流
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String CLOSE_STREAMS = URL_PREFIX + "close_streams";
    /**
     * 获取Session列表
     */
    public static final String GET_ALL_SESSION = URL_PREFIX + "getAllSession";
    /**
     * 断开tcp连接
     * @param id 客户端唯一id，可以通过getAllSession接口获取
     */
    public static final String KICK_SESSION = URL_PREFIX + "kick_session";
    /**
     * 批量断开tcp连接
     */
    public static final String KICK_SESSIONS = URL_PREFIX + "kick_sessions";
    /**
     * 添加rtsp/rtmp/hls拉流代理
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param url 拉流地址，例如rtmp://live.hkstv.hk.lxdns.com/live/hks2
     */
    public static final String ADD_STREAM_PROXY = URL_PREFIX + "addStreamProxy";
    /**
     * 关闭拉流代理
     * @param key addStreamProxy接口返回的key
     */
    public static final String DEL_STREAM_PROXY = URL_PREFIX + "delStreamProxy";
    /**
     * 添加rtsp/rtmp推流
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param dstUrl 推流地址，需要与schema字段协议一致
     */
    public static final String ADD_STREAM_PUSHER_PROXY = URL_PREFIX + "addStreamPusherProxy";
    /**
     * 关闭推流
     * @param key addStreamProxy接口返回的key
     */
    public static final String DEL_STREAM_PUSHER_PROXY = URL_PREFIX + "delStreamPusherProxy";
    /**
     * 添加FFmpeg拉流代理
     * @param srcUrl FFmpeg拉流地址,支持任意协议或格式(只要FFmpeg支持即可)
     * @param dstUrl FFmpeg rtmp推流地址，一般都是推给自己，例如rtmp://127.0.0.1/live/stream_form_ffmpeg
     * @param timeoutMs FFmpeg推流成功超时时间,单位毫秒
     * @param enableHls 是否开启hls录制
     * @param enableMp4 是否开启mp4录制
     */
    public static final String ADD_FFMPEG_SOURCE = URL_PREFIX + "addFFmpegSource";
    /**
     * 关闭FFmpeg拉流代理
     * @param key addFFmpegSource接口返回的key
     */
    public static final String DEL_FFMPEG_SOURCE = URL_PREFIX + "delFFmpegSource";
    /**
     * 流是否在线
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String IS_MEDIA_ONLINE = URL_PREFIX + "isMediaOnline";
    /**
     * 获取媒体流播放器列表
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String GET_MEDIA_PLAYER_LIST = URL_PREFIX + "getMediaPlayerList";
    /**
     * 获取流信息
     * @param schema 协议，例如 rtsp或rtmp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String GET_MEDIA_INFO = URL_PREFIX + "getMediaInfo";
    /**
     * 搜索文件系统，获取流对应的录像文件列表或日期文件夹列表
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param customizedPath 录像文件保存自定义根目录，为空则采用配置文件设置
     * @param period 流的录像日期，格式为2020-02-01,如果不是完整的日期，那么是搜索录像文件夹列表，否则搜索对应日期下的mp4文件列表
     */
    public static final String GET_MP4_RECORD_FILE = URL_PREFIX + "getMp4RecordFile";
    /**
     * 删除录像文件夹
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param period 流的录像日期，格式为2020-02-01,如果不是完整的日期，那么是搜索录像文件夹列表，否则搜索对应日期下的mp4文件列表
     */
    public static final String DELETE_RECORD_DIRECTORY = URL_PREFIX + "deleteRecordDirectory";
    /**
     * 开始录制
     * @param type 0为hls，1为mp4
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String START_RECORD = URL_PREFIX + "startRecord";
    /**
     * 设置录像速度
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param speed 要设置的录像倍速 示例值 2.0
     */
    public static final String SET_RECORD_SPEED = URL_PREFIX + "setRecordSpeed";
    /**
     * 设置录像流播放位置
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param stamp 要设置的录像播放位置
     */
    public static final String SEEK_RECORD_STAMP = URL_PREFIX + "seekRecordStamp";
    /**
     * 停止录制
     * @param type 0为hls，1为mp4
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String STOP_RECORD = URL_PREFIX + "stopRecord";
    /**
     * 是否正在录制
     * @param type 0为hls，1为mp4
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     */
    public static final String IS_RECORDING = URL_PREFIX + "isRecording";
    /**
     * 获取截图
     * @param url 需要截图的url，可以是本机的，也可以是远程主机的
     * @param timeoutSec 截图失败超时时间，防止FFmpeg一直等待截图
     * @param expireSec 截图的过期时间，该时间内产生的截图都会作为缓存返回
     * @return
     */
    public static final String GET_SNAP = URL_PREFIX + "getSnap";
    /**
     * 获取rtp推流信息
     * @param streamId 流id
     */
    public static final String GET_RTP_INFO = URL_PREFIX + "getRtpInfo";
    /**
     * 创建RTP服务器
     * @param port 绑定的端口，0时为随机端口
     * @param tcpMode tcp模式，0时为不启用tcp监听，1时为启用tcp监听，2时为tcp主动连接模式
     * @param streamId 该端口绑定的流id
     */
    public static final String OPEN_RTP_SERVER = URL_PREFIX + "openRtpServer";
    /**
     * 连接RTP服务器
     * @param dstUrl tcp主动模式时服务端地址
     * @param dstPort tcp主动模式时服务端端口
     * @param streamId OpenRtpServer时绑定的流id
     */
    public static final String CONNECT_RTP_SERVER = URL_PREFIX + "connectRtpServer";
    /**
     * 关闭RTP服务器
     * @param streamId 该端口绑定的流id
     */
    public static final String CLOSE_RTP_SERVER = URL_PREFIX + "closeRtpServer";
    /**
     * 暂停RTP超时检查
     * @param streamId 该端口绑定的流id
     */
    public static final String PAUSE_RTP_CHECK = URL_PREFIX + "pauseRtpCheck";
    /**
     * 恢复RTP超时检查
     * @param streamId 该端口绑定的流id
     */
    public static final String RESUME_RTP_CHECK = URL_PREFIX + "resumeRtpCheck";
    /**
     * 获取RTP服务器列表
     */
    public static final String LIST_RTP_SERVER = URL_PREFIX + "listRtpServer";
    /**
     * 开始发送rtp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param ssrc rtp推流的ssrc，ssrc不同时，可以推流到多个上级服务器
     * @param dstUrl 目标ip或域名
     * @param dstPort 目标端口
     * @param isUdp 是否为udp模式,否则为tcp模式
     * @param srcPort 使用的本机端口，为0或不传时默认为随机端口
     * @param pt 发送时，rtp的pt（uint8_t）,不传时默认为96
     * @param usePs 发送时，rtp的负载类型。为1时，负载为ps；为0时，为es；不传时默认为1
     * @param onlyAudio 当use_ps 为0时，有效。为1时，发送音频；为0时，发送视频；不传时默认为0
     */
    public static final String START_SEND_RTP = URL_PREFIX + "startSendRtp";
    /**
     * 开始tcp passive被动发送rtp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param ssrc rtp推流的ssrc，ssrc不同时，可以推流到多个上级服务器
     */
    public static final String START_SEND_RTP_PASSIVE = URL_PREFIX + "startSendRtpPassive";
    /**
     * 停止 发送rtp
     * @param vhost 虚拟主机，例如__defaultVhost__
     * @param app 应用名，例如 live
     * @param stream 流id，例如 test
     * @param ssrc 根据ssrc关停某路rtp推流，置空时关闭所有流
     */
    public static final String STOP_SEND_RTP = URL_PREFIX + "stopSendRtp";
    /**
     * 获取版本信息
     */
    public static final String VERSION = URL_PREFIX + "version";
    /**
     * 修改RTP SSRC信息
     * @param ssrc 虚拟主机，例如__defaultVhost__
     * @param stream_id 应用名，例如 live
     */
    public static final String UPDATE_RTP_SERVER_SSRC = URL_PREFIX + "updateRtpServerSSRC";

    //------------------------------流媒体服务回调事件---开始------------------------------
    //回调url前缀
    public static final String HOOK_URL_PREFIX = "/index/hook";
    //流量统计事件，播放器或推流器断开时并且耗用流量超过特定阈值时会触发此事件，阈值通过配置文件
    public static final String MEDIA_HOOK_ON_FLOW_REPORT="on_flow_report";
    //访问http文件服务器上hls之外的文件时触发。
    public static final String MEDIA_HOOK_ON_HTTP_ACCESS="on_http_access";
    //播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件； 如果流不存在，那么先触发on_play事件然后触发on_stream_not_found事件。 播放rtsp流时，如果该流启动了rtsp专属鉴权(on_rtsp_realm)那么将不再触发on_play事件。
    public static final String MEDIA_HOOK_ON_PLAY="on_play";
    //rtsp/rtmp/rtp推流鉴权事件。
    public static final String MEDIA_HOOK_ON_PUBLISH="on_publish";
    //录制mp4完成后通知事件；此事件对回复不敏感。
    public static final String MEDIA_HOOK_ON_RECORD_MP4="on_record_mp4";
    //该rtsp流是否开启rtsp专用方式的鉴权事件，开启后才会触发on_rtsp_auth事件。需要指出的是rtsp也支持url参数鉴权，它支持两种方式鉴权。
    public static final String MEDIA_HOOK_ON_RTSP_AUTH="on_rtsp_auth";
    //rtsp专用的鉴权事件，先触发on_rtsp_realm事件然后才会触发on_rtsp_auth事件。
    public static final String MEDIA_HOOK_ON_RTSP_REALM="on_rtsp_realm";
    //shell登录鉴权，ZLMediaKit提供简单的telnet调试方式 使用telnet 127.0.0.1 9000能进入MediaServer进程的shell界面。
    public static final String MEDIA_HOOK_ON_SHELL_LOGIN="on_shell_login";
    //rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
    public static final String MEDIA_HOOK_ON_STREAM_CHANGED="on_stream_changed";
    //流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
    public static final String MEDIA_HOOK_ON_STREAM_NONE_READER="on_stream_none_reader";
    //流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
    public static final String MEDIA_HOOK_ON_STREAM_NOT_FOUND="on_stream_not_found";
    //服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
    public static final String MEDIA_HOOK_ON_SERVER_STARTED="on_server_started";
    //服务器定时上报时间，上报间隔可配置，默认10s上报一次
    public static final String MEDIA_HOOK_ON_SERVER_KEEPALIVE="on_server_keepalive";
    //调用openRtpServer 接口，rtp server 长时间未收到数据,执行此web hook,对回复不敏感
    public static final String MEDIA_HOOK_ON_RTP_SERVER_TIMEOUT="on_rtp_server_timeout";
    //调用openRtpServer 接口，rtp server 长时间未收到数据,执行此web hook,对回复不敏感
    public static final String MEDIA_HOOK_ON_SEND_RTP_STOPPED="on_send_rtp_stopped";
    //------------------------------流媒体服务回调事件---结束------------------------------


}
