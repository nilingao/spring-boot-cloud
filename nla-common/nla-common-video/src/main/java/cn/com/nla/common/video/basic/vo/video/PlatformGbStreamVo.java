package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
    * 国标级联关联直播流
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class PlatformGbStreamVo extends LongIdEntity {
    /**
     * 平台ID
     */
    private String platformId;

    /**
     * 目录ID
     */
    private String catalogId;

    /**
     * 直播流ID
     */
    private Long gbStreamId;
}