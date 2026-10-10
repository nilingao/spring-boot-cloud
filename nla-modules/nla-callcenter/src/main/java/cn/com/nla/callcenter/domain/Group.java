package cn.com.nla.callcenter.domain;

import cn.com.nla.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 技能组表；业务与协议适配留阶段6.6。
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fs_group")
public class Group extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 雪花主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 企业ID。 */
    @TableField(value = "`company_id`")
    private Long companyId;

    /** 技能组名称。 */
    @TableField(value = "`name`")
    private String name;

    /** 话后自动空闲时长。 */
    @TableField(value = "`after_interval`")
    private Integer afterInterval;

    /** 主叫显号号码池。 */
    @TableField(value = "`caller_display_id`")
    private Long callerDisplayId;

    /** 被叫显号号码池。 */
    @TableField(value = "`called_display_id`")
    private Long calledDisplayId;

    /** 1:振铃录音,2:接通录音。 */
    @TableField(value = "`record_type`")
    private Integer recordType;

    /** 技能组优先级。 */
    @TableField(value = "`level_value`")
    private Integer levelValue;

    /** tts引擎id。 */
    @TableField(value = "`tts_engine`")
    private Long ttsEngine;

    /** 转坐席时播放内容。 */
    @TableField(value = "`play_content`")
    private String playContent;

    /** 转服务评价(0:否,1:是)。 */
    @TableField(value = "`evaluate`")
    private Long evaluate;

    /** 排队音。 */
    @TableField(value = "`queue_play`")
    private Long queuePlay;

    /** 转接提示音。 */
    @TableField(value = "`transfer_play`")
    private Long transferPlay;

    /** 外呼呼叫超时时间。 */
    @TableField(value = "`call_time_out`")
    private Integer callTimeOut;

    /** 技能组类型。 */
    @TableField(value = "`group_type`")
    private Integer groupType;

    /** 0:不播放排队位置,1:播放排队位置。 */
    @TableField(value = "`notify_position`")
    private Integer notifyPosition;

    /** 频次。 */
    @TableField(value = "`notify_rate`")
    private Integer notifyRate;

    /** 您前面还有$位用户在等待。 */
    @TableField(value = "`notify_content`")
    private String notifyContent;

    /** 主叫记忆(1:开启,0:不开启)。 */
    @TableField(value = "`call_memory`")
    private Integer callMemory;

    /** 状态。 */
    @TableField(value = "`status`")
    private Integer status;

    /** 删除标志：0存在、1删除。 */
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
