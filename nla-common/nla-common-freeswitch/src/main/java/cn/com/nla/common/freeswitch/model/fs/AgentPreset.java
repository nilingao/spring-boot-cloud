package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentPreset implements Serializable {

    /**
     * 1:当前生效 2:永久生效
     */
    @NotNull(message = "预设类型不能为空")
    @Range(min = 1 , max = 2, message = "预设字段错误")
    private Integer type;


    @NotNull(message = "预设状态不能为空")
    private AgentStateEnum agentStateEnum;
}
