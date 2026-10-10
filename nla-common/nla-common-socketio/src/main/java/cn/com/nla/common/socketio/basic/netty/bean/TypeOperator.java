package cn.com.nla.common.socketio.basic.netty.bean;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public abstract class TypeOperator {

    public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    public void writeString(ByteBuf buffer, String str) {
        if (str == null) {
            buffer.writeInt(-1);
        } else {
            byte[] buf = str.getBytes(CHARSET_UTF_8);
            buffer.writeInt(buf.length);
            buffer.writeBytes(buf);
        }
    }

    public String readString(ByteBuf buffer) {
        int length = buffer.readInt();
        if (length == -1) {
            return null;
        } else {
            validateLength(buffer, length, 1);
            byte[] buf = new byte[length];
            buffer.readBytes(buf);
            return new String(buf, CHARSET_UTF_8);
        }
    }

    public String readString(ByteBuf buffer, int length) {
        if (length == -1) {
            return null;
        } else {
            validateLength(buffer, length, 1);
            byte[] buf = new byte[length];
            buffer.readBytes(buf);
            return new String(buf, CHARSET_UTF_8);
        }
    }

    public void writeStringList(ByteBuf buffer, List<String> list) {
        buffer.writeInt(list.size());
        for (String e : list) {
            writeString(buffer, e);
        }
    }

    public List<String> readStringList(ByteBuf buffer) {
        List<String> list = new ArrayList<String>();
        int len = buffer.readInt();
        validateLength(buffer, len, 4);
        for (int i = 0; i < len; i++) {
            list.add(readString(buffer));
        }

        return list;
    }

    public void writeIntList(ByteBuf buffer, List<Integer> list) {
        buffer.writeInt(list.size());
        for (Integer e : list) {
            buffer.writeInt(e);
        }
    }

    public List<Integer> readIntList(ByteBuf buffer) {
        List<Integer> list = new ArrayList<Integer>();
        int len = buffer.readInt();
        validateLength(buffer, len, 4);
        for (int i = 0; i < len; i++) {
            list.add(buffer.readInt());
        }

        return list;
    }

    public void writeFloatList(ByteBuf buffer, List<Float> list) {
        buffer.writeInt(list.size());
        for (Float e : list) {
            buffer.writeFloat(e);
        }
    }

    public List<Float> readFloatList(ByteBuf buffer) {
        List<Float> list = new ArrayList<Float>();
        int len = buffer.readInt();
        validateLength(buffer, len, 4);
        for (int i = 0; i < len; i++) {
            list.add(buffer.readFloat());
        }

        return list;
    }

    private static void validateLength(ByteBuf buffer, int length, int width) {
        if (length < 0 || (long) length * width > buffer.readableBytes()) {
            throw new CorruptedFrameException("Invalid string/list length: " + length);
        }
    }

    public boolean checkCRC(String checksum, ByteBuf buf) {
        buf.markReaderIndex();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String checkCRC = getCRC(bytes);
        buf.resetReaderIndex();
        if (checkCRC.equals(checksum)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 计算CRC16校验码
     *
     * @param bytes
     * @return
     */
    public static String getCRC(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;

        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= ((int) bytes[i] & 0x000000ff);
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) != 0) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        //结果转换为16进制
        String result = Integer.toHexString(CRC).toUpperCase();
        if (result.length() != 4) {
            StringBuffer sb = new StringBuffer("0000");
            result = sb.replace(4 - result.length(), 4, result).toString();
        }
        /**
         * 第二种方式
         * CRC16Modbus crc16Modbus = new CRC16Modbus();
         * crc16Modbus.update(bytes);
         * result = crc16Modbus.getHexValue(true).toUpperCase();
         */
        return result;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
