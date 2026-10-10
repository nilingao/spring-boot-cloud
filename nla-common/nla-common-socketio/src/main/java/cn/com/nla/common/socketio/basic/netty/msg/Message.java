package cn.com.nla.common.socketio.basic.netty.msg;

import cn.com.nla.common.socketio.basic.netty.bean.TypeOperator;
import cn.hutool.core.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 数据结构说明
 * 第一位 int类型 四个字节 MsgCode code
 * 第二位 扩展时间格式数据   可自定义 writeData readData
 * 第二位 int类型 四个字节 serial 可自定义
 * 第三位 int类型 四个字节 自定义消息体长度
 * 第四位 short类型 两个字节  CRC消息体的密文 可选 可进行校验 如果 false 则没有此字节
 * 第五位 自定义消息体 此消息体可自定位编辑内容
 */
public abstract class Message extends TypeOperator {
    private int serial;

    public abstract int getMsgCode();
    public abstract boolean checkCRC();
    public void readData(ByteBuf buffer) {
    }
    public void writeData(ByteBuf buffer) {
    }
    public void readTime(ByteBuf buffer) {
    }
    public void writeTime(ByteBuf buffer) {
    }

    public final void decode(ByteBuf buffer) {
        int msgCode = buffer.readInt();
        if (msgCode != getMsgCode()) { throw new CorruptedFrameException("Unexpected message code: " + msgCode); }
        readTime(buffer);
        serial = buffer.readInt();
        int length = buffer.readInt();
        int crcLength = checkCRC() ? 2 : 0;
        if (length < 0 || buffer.readableBytes() != (long) length + crcLength) {
            throw new CorruptedFrameException("Invalid message body length");
        }
        if(checkCRC()){
            byte[] crcBytes = new byte[2];//定义crc 2个 字节
            buffer.readBytes(crcBytes);
            String checksum = HexUtil.encodeHexStr(crcBytes).toUpperCase();
            if(!checkCRC(checksum,buffer)){
                throw new CorruptedFrameException("CRC Authentication failed");
            }
        }
        readData(buffer);
    }
    public final void encode(ByteBuf buffer) {
        if(getMsgCode() == 0) {
            throw new IllegalArgumentException("msgCode is zero");
        }
        buffer.writeInt(getMsgCode());
        writeTime(buffer);
        buffer.writeInt(serial);

        int position = buffer.writerIndex();
        buffer.writeInt(0);
        //是否校验异或
        if(checkCRC()) {
            buffer.writeShort(0);
        }
        writeData(buffer);
        if(checkCRC()){
            writeCRC(buffer, position + 4);//4是 writeInt的长度
            buffer.setInt(position, buffer.writerIndex() - position - 4 - 2);
        }else{
            buffer.setInt(position, buffer.writerIndex() - position - 4);
        }
    }

    public void writeCRC(ByteBuf buffer, int position){
        int bodyLength = buffer.writerIndex() - position - 2;
        ByteBuf buf = buffer.slice(position + 2, bodyLength);//复制包体
        byte[] crcBytes = new byte[bodyLength];//定义包体字节
        buf.readBytes(crcBytes);//写入
        String checkSumHex = getCRC(crcBytes) ;//获取到异或
        byte[] bytes = HexUtil.decodeHex(checkSumHex);
        buffer.setBytes(position, bytes);//写入异或
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
