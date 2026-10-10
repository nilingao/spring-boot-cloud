package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.Group;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 技能组表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = Group.class, reverseConvertGenerate = false)
public class GroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 企业ID。 */
    @Min(value = 0, message = "companyId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long companyId;

    /** 技能组名称。 */
    @Size(max = 20, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 话后自动空闲时长。 */
    @Min(value = 0, message = "afterInterval不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer afterInterval;

    /** 主叫显号号码池。 */
    @Min(value = 0, message = "callerDisplayId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long callerDisplayId;

    /** 被叫显号号码池。 */
    @Min(value = 0, message = "calledDisplayId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long calledDisplayId;

    /** 1:振铃录音,2:接通录音。 */
    @Min(value = 0, message = "recordType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer recordType;

    /** 技能组优先级。 */
    @Min(value = 0, message = "levelValue不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer levelValue;

    /** tts引擎id。 */
    @Min(value = 0, message = "ttsEngine不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long ttsEngine;

    /** 转坐席时播放内容。 */
    @Size(max = 100, message = "playContent长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String playContent;

    /** 转服务评价(0:否,1:是)。 */
    @Min(value = 0, message = "evaluate不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long evaluate;

    /** 排队音。 */
    @Min(value = 0, message = "queuePlay不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long queuePlay;

    /** 转接提示音。 */
    @Min(value = 0, message = "transferPlay不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long transferPlay;

    /** 外呼呼叫超时时间。 */
    @Min(value = 0, message = "callTimeOut不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer callTimeOut;

    /** 技能组类型。 */
    @Min(value = 0, message = "groupType不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer groupType;

    /** 0:不播放排队位置,1:播放排队位置。 */
    @Min(value = 0, message = "notifyPosition不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer notifyPosition;

    /** 频次。 */
    @Min(value = 0, message = "notifyRate不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer notifyRate;

    /** 您前面还有$位用户在等待。 */
    @Size(max = 255, message = "notifyContent长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String notifyContent;

    /** 主叫记忆(1:开启,0:不开启)。 */
    @Min(value = 0, message = "callMemory不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Integer callMemory;

    /** 状态。 */
    private Integer status;
}
