package cn.com.nla.system.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行政区划查询业务对象 sys_area
 * <p>
 * 行政区划为只读参考数据，本对象仅用于列表/树查询条件绑定，无写入场景。
 *
 * @author TZY
 */
@Data
public class SysAreaBo implements Serializable {

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

}
