package cn.com.nla.face.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.face.domain.Person;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人员业务对象；特征不接受表单输入，由后续识别服务生成。
 *
 * @author TZY
 */
@Data
@AutoMapper(target = Person.class, reverseConvertGenerate = false)
public class PersonBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 新增时由框架生成，修改时必填。 */
    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "人员主键不能为空", groups = EditGroup.class)
    private Long id;

    /** 图片自定义编号。 */
    @NotBlank(message = "图片编号不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 40, message = "图片编号不能超过40个字符", groups = {AddGroup.class, EditGroup.class})
    private String imgId;

    /** 图片地址。 */
    @NotBlank(message = "图片地址不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 256, message = "图片地址不能超过256个字符", groups = {AddGroup.class, EditGroup.class})
    private String imgUrl;

    /** 人员姓名。 */
    @NotBlank(message = "人员姓名不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 40, message = "人员姓名不能超过40个字符", groups = {AddGroup.class, EditGroup.class})
    private String personName;

    /** 年龄。 */
    @Min(value = 0, message = "年龄不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer personAge;

    /** 性别：0 未知、1 男、2 女。 */
    @NotNull(message = "性别不能为空", groups = {AddGroup.class, EditGroup.class})
    @Min(value = 0, message = "性别必须为0、1或2", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 2, message = "性别必须为0、1或2", groups = {AddGroup.class, EditGroup.class})
    private Integer gender;

    /** 地址，允许空字符串。 */
    @NotNull(message = "地址不能为null", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 256, message = "地址不能超过256个字符", groups = {AddGroup.class, EditGroup.class})
    private String address;
}
