package cn.com.nla.common.video.basic.vo.media;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HookVo implements Serializable {
    /**
     * 流媒体唯一编码
     */
    private String mediaServerId;
    /**
     * 推流鉴权Id
     */
    private String callId;

}
