package cn.com.nla.common.freeswitch.model.fs;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class VdnConfigInfo implements Serializable,Comparable<VdnConfigInfo> {
    /**
     * PK
     */
    private Long id;

    /**
     * 创建时间
     */
    private Long cts;

    /**
     * 修改时间
     */
    private Long uts;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 子码日程
     */
    private String name;

    /**
     * vdn_id
     */
    private String vdnId;

    /**
     * 日程id
     */
    private Long scheduleId;

    /**
     * 路由类型(1:技能组,2:放音,3:ivr,4:坐席,5:外呼)
     */
    private Integer routeType;

    /**
     * 路由类型值
     */
    private String routeValue;

    /**
     * 放音类型(1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机)
     */
    private Integer playType;

    /**
     * 放音类型对应值
     */
    private String playValue;

    /**
     * 结束音
     */
    private String dtmfEnd;

    /**
     * 重复播放次数
     */
    private Integer retry;

    /**
     * 最大收键长度
     */
    private Integer dtmfMax;

    /**
     * 最小收键长度
     */
    private Integer dtmfMin;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 每个字码日程有多个按键导航
     */
    private List<VdnDtmfInfo> dtmfList;

    /**
     * 日程
     */
    private VdnScheduleInfo vdnScheduleInfo;

    @Override
    public int compareTo(VdnConfigInfo o) {
        return this.getVdnScheduleInfo().getLevelValue() - o.getVdnScheduleInfo().getLevelValue();
    }

    public boolean isEffectiveSchedule() {
        if (vdnScheduleInfo == null) {
            return false;
        }
        DateTime date = DateUtil.date();
        int week = DateUtil.dayOfWeek(date);
        switch (week) {
            case 1:
                if (this.vdnScheduleInfo.getSun() == 0) {
                    return false;
                }
                break;
            case 2:
                if (this.vdnScheduleInfo.getMon() == 0) {
                    return false;
                }
                break;

            case 3:
                if (this.vdnScheduleInfo.getTue() == 0) {
                    return false;
                }
                break;

            case 4:
                if (this.vdnScheduleInfo.getWed() == 0) {
                    return false;
                }
                break;

            case 5:
                if (this.vdnScheduleInfo.getThu() == 0) {
                    return false;
                }
                break;

            case 6:
                if (this.vdnScheduleInfo.getFri() == 0) {
                    return false;
                }
                break;

            case 7:
                if (this.vdnScheduleInfo.getSat() == 0) {
                    return false;
                }
                break;
            default:
                break;
        }


        //判断是否在生效日期内
        if (StringUtils.isNotBlank(vdnScheduleInfo.getStartDay())) {
            DateTime startDate = DateUtil.parse(vdnScheduleInfo.getStartDay(), DatePattern.NORM_DATE_PATTERN, DatePattern.NORM_DATETIME_MINUTE_PATTERN, DatePattern.NORM_DATETIME_PATTERN);
            if (vdnScheduleInfo.getType() == 1) {
                //相对时间
                if (DateUtil.beginOfDay(date).isBefore(DateUtil.beginOfDay(startDate))) {
                    return false;
                }
            } else if (date.isBefore(startDate)) {
                return false;
            }
        }
        if (StringUtils.isNotBlank(vdnScheduleInfo.getEndDay())) {
            DateTime endDate = DateUtil.parse(vdnScheduleInfo.getEndDay(), DatePattern.NORM_DATE_PATTERN, DatePattern.NORM_DATETIME_MINUTE_PATTERN, DatePattern.NORM_DATETIME_PATTERN);
            if (vdnScheduleInfo.getType() == 1) {
                //相对时间
                //相对时间
                if (DateUtil.beginOfDay(date).isAfter(DateUtil.beginOfDay(endDate))) {
                    return false;
                }
            } else if (date.isAfter(endDate)) {
                return false;
            }
        }

        //判断是否在生效的时间范围内（当天）
        LocalTime nowTime = LocalTime.now();
        if (StringUtils.isNotBlank(vdnScheduleInfo.getStartTime())) {
            LocalTime startTime = LocalTime.parse(vdnScheduleInfo.getStartTime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (startTime.isAfter(nowTime)) {
                return false;
            }
        }
        if (StringUtils.isNotBlank(vdnScheduleInfo.getEndTime())) {
            LocalTime endTime = LocalTime.parse(vdnScheduleInfo.getEndTime(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (endTime.isBefore(nowTime)) {
                return false;
            }
        }
        return true;
    }
}
