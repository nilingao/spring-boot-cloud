package cn.com.nla.common.freeswitch.model.fs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class VdnCodeInfo implements Serializable {

    /**
     * PK
     */
    private Long id;

    /**
     * 企业ID
     */
    private Long companyId;

    /**
     * vdn名称
     */
    private String name;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 每个vdn有多个日程-字码
     */
    private List<VdnConfigInfo> vdnSchedulePoList;

    /**
     * 获取有效日程
     *
     * @return
     */
    public VdnConfigInfo getEffectiveSchedule() {
        if (this.vdnSchedulePoList.size() == 0) {
            return null;
        }
        // vdnSchedulePoList已经按照日程的优先级排序了
        for (VdnConfigInfo vdnSchedulePo : vdnSchedulePoList) {
            if (vdnSchedulePo.isEffectiveSchedule()) {
                return vdnSchedulePo;
            }
        }
        return null;
    }
}
