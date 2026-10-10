package cn.com.nla.face.domain.vo;

import cn.com.nla.face.domain.Person;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 人员视图对象，不包含内部人脸特征或删除标志。
 *
 * @author TZY
 */
@Data
@AutoMapper(target = Person.class)
public class PersonVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String imgId;
    private String imgUrl;
    private String personName;
    private Integer personAge;
    private Integer gender;
    private String address;
    private Long createDept;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
