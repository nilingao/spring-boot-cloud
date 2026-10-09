package cn.com.nla.common.freeswitch.model.fs;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CompanyInfo implements Serializable {

    /**
     * PK
     */
    private String id;

    /**
     * 创建时间
     */
    private Long cts;

    /**
     * 修改时间
     */
    private Long uts;

    /**
     * 名称
     */
    private String name;

    /**
     * 父企业ID
     */
    private String idPath;

    /**
     * 父企业
     */
    private Long pid;

    /**
     * 简称
     */
    private String companyCode;

    /**
     * 时区概念(默认是GTM+8)
     */
    private Integer gmt;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 电话
     */
    private String phone;

    /**
     * 金额
     */
    private Long balance;

    /**
     * 1:呼出计费,2:呼入计费,3:双向计费,0:全免费
     */
    private Integer billType;

    /**
     * 0:预付费;1:后付费
     */
    private Integer payType;

    /**
     * 隐藏客户号码(0:不隐藏;1:隐藏)
     */
    @Builder.Default
    private Integer hiddenCustomer = 0;

    /**
     * 坐席密码等级(1:不限制 2:数字和字母 3:大小写字母和数字组合)
     */
    private Integer secretType;

    /**
     * 验证秘钥
     */
    private String secretKey;

    /**
     * IVR通道数
     */
    private Integer ivrLimit;

    /**
     * 开通坐席
     */
    private Integer agentLimit;

    /**
     * 开通技能组
     */
    private Integer groupLimit;

    /**
     * 单技能组中坐席上限
     */
    private Integer groupAgentLimit;

    /**
     * 录音保留x个月
     */
    private Integer recordStorage;

    /**
     * 话单回调通知
     */
    private String notifyUrl;

    /**
     * 状态(0:禁用企业,1:免费企业;2:试用企业,3:付费企业)
     */
    private Integer status;

    /**
     * 企业技能组
     */
    private List<Long> groupIds;


    /**
     * 企业号码池
     */
    private List<CompanyDisplay> companyDisplays;

    /**
     * vdnId - vdnCode
     */
    private Map<Long, VdnCodeInfo> vdnCodeMap;


    /**
     * 企业路由字冠集合
     */
    private Map<String, RouteGroupInfo> routeGroupMap;

    /**
     * 企业坐席数量
     */
    private Integer agentSize;

    public void initRouteCalls(List<RouteGroupInfo> routeGroupInfoList,List<RouteCallInfo> routeCallInfoList) {
        if(routeCallInfoList == null || routeCallInfoList.isEmpty() || routeGroupInfoList == null || routeGroupInfoList.isEmpty()){
            return;
        }

        Map<Long, RouteCallInfo> routeGroupIdMap = routeCallInfoList.stream().collect(Collectors.toMap(RouteCallInfo::getRouteGroupId, o -> o));
        routeGroupMap = routeGroupInfoList.stream().filter(o -> routeGroupIdMap.containsKey(o.getId())).collect(Collectors.toMap(o->routeGroupIdMap.get(o.getId()).getRouteNum(),o->o));
    }

    public void initVdn(List<VdnCodeInfo> vdnCodeInfoList, List<VdnConfigInfo> vdnConfigInfoList,List<VdnScheduleInfo> vdnScheduleInfoList,List<VdnDtmfInfo> vdnDtmfInfoList) {
        Map<String, List<VdnConfigInfo>> vdnConfigInfoMap = vdnConfigInfoList.stream().collect(Collectors.groupingBy(VdnConfigInfo::getVdnId));
        Map<Long, VdnScheduleInfo> vdnScheduleInfoMap = vdnScheduleInfoList.stream().collect(Collectors.toMap(VdnScheduleInfo::getId, o -> o));
        Map<Long, List<VdnDtmfInfo>> vdnDtmfInfoMap = vdnDtmfInfoList.stream().collect(Collectors.groupingBy(VdnDtmfInfo::getNavigateId));
        for (VdnCodeInfo vdnCodeInfo : vdnCodeInfoList) {
            if (vdnCodeInfo.getStatus() == ConstEnum.Flag.NO.getValue()) {
                continue;
            }
            List<VdnConfigInfo> configInfoList = vdnConfigInfoMap.computeIfAbsent(String.valueOf(vdnCodeInfo.getId()), o -> new ArrayList<>());
            for (VdnConfigInfo vdnConfigInfo : configInfoList) {
                VdnScheduleInfo vdnScheduleInfo = vdnScheduleInfoMap.get(vdnConfigInfo.getScheduleId());
                if(vdnScheduleInfo !=null){
                    vdnConfigInfo.setVdnScheduleInfo(vdnScheduleInfo);
                }
                vdnConfigInfo.setDtmfList(vdnDtmfInfoMap.computeIfAbsent(vdnConfigInfo.getId(),v->new ArrayList<>()));
            }
            vdnCodeInfo.setVdnSchedulePoList(configInfoList.stream().filter(o -> o.getVdnScheduleInfo() != null).collect(Collectors.toList()));

        }
        vdnCodeMap = vdnCodeInfoList.stream().collect(Collectors.toMap(VdnCodeInfo::getId, o->o));
    }
}
