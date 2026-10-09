package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaybackInfo  implements Serializable {

    /**
     * PK
     */
    private String id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * 放音文件
     */
    private String playback;

    /**
     * 1:待审核,2:审核通过
     */
    private Integer status;
}
