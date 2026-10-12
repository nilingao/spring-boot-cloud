package cn.com.nla.video.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备订阅状态，数据来源于订阅缓存。
 *
 * @author TZY
 */
@Data
public class VideoSubscribeInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 报警订阅是否生效
     */
    private Boolean alarm;

    /**
     * 目录订阅是否生效
     */
    private Boolean catalog;

    /**
     * 移动位置订阅是否生效
     */
    private Boolean mobilePosition;
}
