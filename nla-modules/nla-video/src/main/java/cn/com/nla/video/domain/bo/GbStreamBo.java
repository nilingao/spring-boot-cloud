package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.GbStream;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 直播流关联国标上级平台输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = GbStream.class, reverseConvertGenerate = false)
public class GbStreamBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long gbStreamId;

    /** 应用名。 */
    @NotBlank(message = "app不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "app长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String app;

    /** 流ID。 */
    @NotBlank(message = "stream不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "stream长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String stream;

    /** 国标ID。 */
    @NotBlank(message = "gbId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "gbId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String gbId;

    /** 名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 经度。 */
    private Double longitude;

    /** 纬度。 */
    private Double latitude;

    /** 流类型（1.拉流/2.推流）。 */
    private Integer streamType;

    /** 流媒体ID。 */
    @Size(max = 50, message = "mediaServerId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String mediaServerId;
}
