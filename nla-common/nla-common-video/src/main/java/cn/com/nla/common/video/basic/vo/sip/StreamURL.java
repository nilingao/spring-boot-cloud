package cn.com.nla.common.video.basic.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 流地址信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamURL implements Serializable {

    /**
     * 协议
     */
    private String protocol;
    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口
     */
    private int port = -1;

    /**
     * 定位位置
     */
    private String file;

    /**
     * 拼接后的地址
     */
    private String url;

    public StreamURL(String protocol, String host, int port, String file) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = file;
        this.url = String.format("%s://%s:%s/%s",protocol,host,port,file);
    }


    @Override
    public String toString() {
        if (protocol != null && host != null && port != -1 ) {
            return String.format("%s://%s:%s/%s", protocol, host, port, file);
        }else {
            return null;
        }
    }
    @Override
    public StreamURL clone() throws CloneNotSupportedException {
        return (StreamURL) super.clone();
    }
}
