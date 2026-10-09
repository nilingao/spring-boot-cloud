package cn.com.nla.common.freeswitch.enums.fs;
public enum NextTypeEnum {

    NORNAL,
    /**
     * 呼叫另外一侧
     */
    NEXT_CALL_OTHER(),

    /**
     * 桥接
     */
    NEXT_CALL_BRIDGE(),

    /**
     * 咨询（转接）通话
     */
    NEXT_CONSULT(),

    /**
     * 完成转接
     */
    NEXT_TRANSFER_SUCCESS(),

    /**
     * 电话转接会议
     */
    NEXT_TRANSFER_CONFERENCE_CALL(),

    /**
     * 强插电话
     */
    NEXT_INSERT_CALL(),

    /**
     * 监听电话
     */
    NEXT_LISTEN_CALL(),

    /**
     * 耳语电话
     */
    NEXT_WHISPER_CALL(),

    /**
     * 进vdn
     */
    NEXT_VDN(),

    /**
     * 进技能组
     */
    NEXT_GROUP(),

    /**
     * 进ivr
     */
    NEXT_IVR(),

    /**
     * 挂机处理
     */
    NEXT_HANGUP(),

    /**
     * 放音queue
     */
    NEXT_QUEUE_PLAY(),
    /**
     * 溢出队列
     */
    NEXT_QUEUE_OVERFLOW_GROUP(),
    /**
     * 溢出进IVR
     */
    NEXT_QUEUE_OVERFLOW_IVR(),
    /**
     * 溢出进vdn
     */
    NEXT_QUEUE_OVERFLOW_VDN(),
    /**
     * 停止放音
     */
    NEXT_QUEUE_PLAY_STOP(),
    /**
     * 应答电话
     */
    NEXT_ANSWER_CALL();
}
