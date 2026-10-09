package cn.com.nla.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行政区划对象 sys_area
 * <p>
 * 国标行政区划参考数据（省/市/区县/街道），只读、批量导入，故不继承 {@code BaseEntity}（无审计列），
 * 主键 {@code areaId} 由外部数据提供（{@link IdType#INPUT}），非雪花自增。
 *
 * @author TZY
 */
@Data
@TableName("sys_area")
public class SysArea implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地区Id
     */
    @TableId(value = "area_id", type = IdType.INPUT)
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
