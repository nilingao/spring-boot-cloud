package cn.com.nla.face.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;

/**
 * 人员信息 face_person；特征由识别服务写入，普通查询不加载特征。
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("face_person")
public class Person extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 图片自定义编号。 */
    private String imgId;

    /** 图片地址。 */
    private String imgUrl;

    /** 人脸特征数组字符串；内部特征查询显式加载，避免普通列表携带大字段。 */
    @TableField(value = "`extract`", select = false)
    @ToString.Exclude
    private String extract;

    /** 人员姓名。 */
    private String personName;

    /** 年龄，可空。 */
    private Integer personAge;

    /** 性别：0 未知、1 男、2 女。 */
    private Integer gender;

    /** 地址。 */
    private String address;

    /** 删除标志：0 存在、1 删除。 */
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
