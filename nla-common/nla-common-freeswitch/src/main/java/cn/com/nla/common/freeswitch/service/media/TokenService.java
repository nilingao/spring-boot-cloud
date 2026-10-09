package cn.com.nla.common.freeswitch.service.media;

public interface TokenService {

    /**
     * token 鉴权，是否有权播放
     * @param token
     * @return
     */
    boolean authentication(String token);

}
