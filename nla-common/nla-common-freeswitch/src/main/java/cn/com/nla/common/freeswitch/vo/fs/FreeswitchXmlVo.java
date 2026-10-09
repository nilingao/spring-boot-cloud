package cn.com.nla.common.freeswitch.vo.fs;

import cn.com.nla.common.freeswitch.enums.fs.FsTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于组装xml时所需要参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreeswitchXmlVo {
    /**
     * 获取xml类型
     */
    private FsTypeEnum fsTypeEnum;
    /**
     * xml模板所需要的参数
     */
    Map<String, Object> modelMap = new HashMap<>();

}
