package cn.com.nla.common.video.basic.vo.sip;

import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author TZY
 */
@Data
public class CatalogData implements Serializable {
    /**
     * 命令序列号
     */
    private int sn;
    /**
     * 总数
     */
    private Integer total;
    /**
     * 错误提示
     */
    private String errorMsg;
    /**
     * 同步的设备
     */
    private DeviceVo deviceVo;
    /**
     * 已同步的设备通道
     */
    private List<DeviceChannelVo> channelList = new ArrayList<>();

}
