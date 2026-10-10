package cn.com.nla.common.video.basic.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GPSMsgInfo {

    /**
     *
     */
    private String id;

    /**
     * 经度 (必选)
     */
    private double lng;

    /**
     * 纬度 (必选)
     */
    private double lat;

    /**
     * 速度,单位:km/h (可选)
     */
    private double speed;

    /**
     * 产生通知时间, 时间格式： 2020-01-14T14:32:12
     */
    private String time;

    /**
     * 方向,取值为当前摄像头方向与正北方的顺时针夹角,取值范围0°~360°,单位:(°)(可选)
     */
    private String direction;

    /**
     * 海拔高度,单位:m(可选)
     */
    private String altitude;

    private boolean stored;
}
