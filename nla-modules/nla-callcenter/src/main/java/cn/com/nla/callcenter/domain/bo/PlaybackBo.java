package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.Playback;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 语音文件表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Playback.class, reverseConvertGenerate = false)
public class PlaybackBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @NotNull(message = "companyId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 放音文件。 */
    @Size(max = 255, message = "playback长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String playback;

    /** 1:待审核,2:审核通过。 */
    @Min(value = 0, message = "status不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer status;
}
