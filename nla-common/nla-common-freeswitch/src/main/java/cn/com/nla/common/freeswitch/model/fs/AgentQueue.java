package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AgentQueue implements Comparable<AgentQueue>{
    private Long priority;

    private String agentKey;

    @Override
    public int compareTo(AgentQueue o) {
        return o.priority.compareTo(this.priority);
    }
}
