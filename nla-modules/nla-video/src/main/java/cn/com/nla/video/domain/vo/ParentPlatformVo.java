package cn.com.nla.video.domain.vo;

import cn.com.nla.video.domain.ParentPlatform;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 平台信息视图，不包含凭据、原始流地址或删除标志。
 * @author TZY
 */
@Data
@AutoMapper(target = ParentPlatform.class)
public class ParentPlatformVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer enable;
    private String name;
    private String serverGbId;
    private String serverGbDomain;
    private String serverIp;
    private Integer serverPort;
    private String username;
    private String deviceGbId;
    private String deviceIp;
    private Integer devicePort;
    private Integer expires;
    private Integer keepTimeout;
    private Integer transport;
    private Integer characterSet;
    private String catalogId;
    private Integer catalogGroup;
    private Integer ptz;
    private Integer rtcp;
    private Integer status;
    private Integer startOfflinePush;
    private String administrativeDivision;
    private Integer treeType;
    private Integer asMessageChannel;

    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
