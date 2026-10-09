package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.CompanyConferenceInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CompanyConferenceInfoManager {

    private String FS_COMPANY_CONFERENCE_INFO = RedisConstant.FS_COMPANY_CONFERENCE_INFO;
    public void put(CompanyConferenceInfo model){
        if(model == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(model.getCompanyId(),model.getCode()),model);
    }


    public CompanyConferenceInfo get(String companyId,String conferenceCode) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(companyId,conferenceCode) + "*"));
        if (!scan.isEmpty()) {
            return (CompanyConferenceInfo)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    public void del(String companyId,String conferenceCode){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(companyId,conferenceCode) + "*"));
        for (String key : scan) {
            RedisUtils.deleteObject(key);
        }
    }

    public void delAll(){
        del("*","*");
    }

    private String getKey(String companyId,String conferenceCode){
        if(companyId == null){
            companyId ="*";
        }
        if(conferenceCode == null){
            conferenceCode ="*";
        }
        return String.format("%s%s:%s",FS_COMPANY_CONFERENCE_INFO,companyId,conferenceCode);
    }


}
