package cn.com.nla.common.video.basic.enums;

public enum EventResultType{
        // 超时
        timeout,
        // 回复
        response,
        // 事务已结束
        transactionTerminated,
        // 会话已结束
        dialogTerminated,
        // 设备未找到
        deviceNotFoundEvent,
        //消息返回
        restResultEvent
}