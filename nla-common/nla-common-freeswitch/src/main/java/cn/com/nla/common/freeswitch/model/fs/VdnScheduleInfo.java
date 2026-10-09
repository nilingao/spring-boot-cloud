package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * 日程表
 */
@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class VdnScheduleInfo implements Serializable {
    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 日程名称
     */
    private String name;

    /**
     * 优先级
     */
    private Integer levelValue;

    /**
     * 1:指定时间,2:相对时间
     */
    private Integer type;

    /**
     * 开始日期
     */
    private String startDay;

    /**
     * 结束日期
     */
    private String endDay;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 周一
     */
    private Integer mon;

    /**
     * 周二
     */
    private Integer tue;

    /**
     * 周三
     */
    private Integer wed;

    /**
     * 周四
     */
    private Integer thu;

    /**
     * 周五
     */
    private Integer fri;

    /**
     * 周六
     */
    private Integer sat;

    /**
     * 周天
     */
    private Integer sun;

    /**
     * 状态
     */
    private Integer status;
}