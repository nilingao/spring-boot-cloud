package cn.com.nla.common.freeswitch.model.call;


import cn.com.nla.common.freeswitch.enums.fs.NextTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created by caoliang on 2020/12/21
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class NextCommand implements Serializable {

    /**
     * 记录执行设备
     */
    private String deviceId;

    /**
     * 下一步执行命令
     */
    private NextTypeEnum nextType;

    /**
     * 执行参数
     */
    private String nextValue;
}
