package cn.com.nla.common.freeswitch.model.call;

import cn.com.nla.common.freeswitch.common.fs.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CallDetail implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = Constant.DATE_TIME_FORMAT)
    @JsonFormat(pattern =  Constant.DATE_TIME_FORMAT)
    private LocalDateTime endTime;

    /**
     * 通话ID
     */
    private String callId;

    /**
     * 顺序
     */
    private Integer detailIndex;

    /**
     * 类型(1:进vdn,2:进ivr,3:技能组,4:按键收号,5:外线)
     */
    private Integer transferType;

    /**
     * 转接ID
     */
    private String transferId;

    /**
     * 出队列原因:排队挂机或者转坐席
     */
    private String reason;

    private String month;

    /**
     * 状态
     */
    private Integer status;
}
