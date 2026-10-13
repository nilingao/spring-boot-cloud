package cn.com.nla.callcenter.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 管理域下拉选项视图, 供前端选择器使用(网关/路由组/平台等轻量形态)。
 *
 * @author TZY
 */
@Data
@Builder
public class FsOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务主键, 统一以字符串承载避免前端精度丢失。 */
    private String id;

    /** 展示名称。 */
    private String name;

    /** 辅助码值(网关路由号/媒体地址等, 视来源域而定)。 */
    private String code;

    /** 状态(启用/在线等, 视来源域而定)。 */
    private Integer status;

}
