package cn.com.nla.common.video.basic.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Protocol model identity and audit data; persistence belongs to the consuming module.
 * @author TZY
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class StringIdEntity implements Serializable {
    private String id;
    private Long createUserId;
    private LocalDateTime createTime;
    private Long updateUserId;
    private LocalDateTime updateTime;
}
