package cn.com.nla.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import cn.com.nla.system.domain.SysArea;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行政区划视图对象 sys_area
 *
 * @author TZY
 */
@Data
@AutoMapper(target = SysArea.class)
public class SysAreaVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地区Id
     */
    private Long areaId;

    /**
     * 父级地区Id（0为顶级）
     */
    private Long parentId;

    /**
     * 地区编码
     */
    private String areaCode;

    /**
     * 地区名称
     */
    private String areaName;

    /**
     * 地区级别（1省 2市 3区县 4街道）
     */
    private Integer level;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 城市中心点（经纬度坐标）
     */
    private String center;

}
