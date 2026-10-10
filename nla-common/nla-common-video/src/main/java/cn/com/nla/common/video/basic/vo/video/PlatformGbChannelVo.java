package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
    * 国标级联关联通道信息
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PlatformGbChannelVo extends LongIdEntity {
    /**
     * 平台国标ID
     */
    private String platformId;

    /**
     * 目录ID
     */
    private String catalogId;

    /**
     * deviceChannel的数据库自增ID
     */
    private Long deviceChannelId;
}