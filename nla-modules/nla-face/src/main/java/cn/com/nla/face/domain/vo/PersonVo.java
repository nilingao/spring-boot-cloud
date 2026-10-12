package cn.com.nla.face.domain.vo;

import cn.com.nla.face.domain.Person;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人员视图对象，不包含内部人脸特征或删除标志。
 *
 * @author TZY
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Person.class)
public class PersonVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    private Long id;
    @ExcelProperty(value = "图片编号")
    private String imgId;
    @ExcelProperty(value = "图片地址")
    private String imgUrl;
    @ExcelProperty(value = "人员姓名")
    private String personName;
    @ExcelProperty(value = "年龄")
    private Integer personAge;
    /** 性别：0 未知、1 男、2 女。 */
    @ExcelProperty(value = "性别")
    private Integer gender;
    @ExcelProperty(value = "地址")
    private String address;
    private Long createDept;
    private Long createBy;
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
