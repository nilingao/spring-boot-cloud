package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CallQueue implements Comparable<CallQueue>{

    private Long priority;

    private String callId;
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime startTime;

    private String groupId;

    private GroupOverFlowInfo groupOverflowInfo;

    private String deviceId;

    private boolean play;

    @Override
    public int compareTo(CallQueue o) {
        return priority.compareTo(this.priority);
    }
}
