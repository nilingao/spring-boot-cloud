package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
    * 直播流关联国标上级平台
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class GbStreamVo extends LongIdEntity {
    /**
     * 主键id
     */
    private Long gbStreamId;

    /**
     * 应用名
     */
    private String app;

    /**
     * 流ID
     */
    private String stream;

    /**
     * 国标ID
     */
    private String gbId;

    /**
     * 名称
     */
    private String name;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 流类型（1.拉流/2.推流）
     */
    private Integer streamType;

    /**
     * 流媒体ID
     */
    private String mediaServerId;

    /**
     * 平台国标ID
     */
    private String platformId;

    /**
     * 目录ID
     */
    private String catalogId;
}