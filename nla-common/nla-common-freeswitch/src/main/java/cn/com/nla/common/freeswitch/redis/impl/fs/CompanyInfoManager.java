package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.CompanyInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGateWayInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGroupInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnCodeInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class CompanyInfoManager {
    private String FS_COMPANY_INFO = RedisConstant.FS_COMPANY_INFO;


    public void put(CompanyInfo model){
        if(model == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(model.getId()),model);
    }


    public CompanyInfo get(String companyId) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(companyId) + "*"));
        if (!scan.isEmpty()) {
            return (CompanyInfo)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    public RouteGateWayInfo getRouteGateWayInfo(String companyId, String called){
        CompanyInfo companyInfo = get(companyId);
        if(companyInfo == null){
            return null;
        }
        //先匹配最长的。
        Set<String> routeGroupKey = companyInfo.getRouteGroupMap().keySet();
        if(routeGroupKey == null || routeGroupKey.isEmpty()){
            return null;
        }
        String route = routeGroupKey.stream().filter(called::contains).max(Comparator.comparingInt(String::length)).orElse(null);
        if(StringUtils.isEmpty(route)){
            return null;
        }
        RouteGroupInfo routeGroup = companyInfo.getRouteGroupMap().get(route);
        if(routeGroup == null || CollectionUtils.isEmpty(routeGroup.getRouteGateWayInfoList())){
            return null;
        }
        return routeGroup.getRouteGateWayInfoList().get(0);
    }

    public VdnCodeInfo getVdnCodeInfo(String companyId, Long vdnId){
        CompanyInfo companyInfo = get(companyId);
        if(companyInfo == null){
            return null;
        }
        return companyInfo.getVdnCodeMap().get(vdnId);
    }

    public void del(String companyId){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(companyId) + "*"));
        for (String key : scan) {
            RedisUtils.deleteObject(key);
        }
    }

    public void delAll(){
        del("*");
    }

    private String getKey(String companyId){
        return String.format("%s%s",FS_COMPANY_INFO,companyId);
    }
}
