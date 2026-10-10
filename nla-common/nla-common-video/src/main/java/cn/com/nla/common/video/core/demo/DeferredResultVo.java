package cn.com.nla.common.video.core.demo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 异步请求回复信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeferredResultVo implements Serializable {

    /**
     * 是否释放全部
     */
    private Integer onAll;
    /**
     * 请求key
     */
    private String key;
    /**
     * 请求id
     */
    private String id;
    /**
     * 请求体
     */
    private Object data;
}
