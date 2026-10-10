package cn.com.nla.common.video.basic.vo.media;

import cn.com.nla.common.video.basic.common.DateFormats;
import cn.com.nla.common.video.basic.enums.HookType;
import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@EqualsAndHashCode(of = {"hookType", "content"})
@NoArgsConstructor
public class HookKey {

    /**
     * 获取hook类型
     */
    private HookType hookType;

    /**
     * 获取hook的具体内容
     */
    private Map<String,Object> content;

    /**
     * 过期时间
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private Date expires;

    public HookKey (HookType hookType,Map<String,Object> content){
        this.hookType = hookType;
        this.content = content;
        this.expires = DateUtil.offsetMinute(new Date(),5);//默认5分钟过期
    }

    public HookKey (HookType hookType,Map<String,Object> content,Date expires){
        if(expires == null){
            throw new RuntimeException("HookKey  expires is null ");
        }
        this.hookType = hookType;
        this.content = content;
        this.expires = expires;
    }

    public void updateExpires(Date expires){
        if(expires == null){
            this.expires = DateUtil.offsetMinute(new Date(),5);//默认5分钟过期
        }else {
            this.expires = expires;
        }

    }
}
