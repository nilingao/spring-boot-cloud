package cn.com.nla.common.freeswitch.enums.sip;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum VideoStreamType {
	//RTP参数
	RTP(0,0,"rtp",null),
	RTP_STREAM(1,0,"RTP_STREAM",null),

	//音频流
	CALL_AUDIO_PHONE(2,8,"CALL_AUDIO_PHONE","PUSH_AUDIO_RTP_STREAM"),
	//视频流
	CALL_VIDEO_PHONE(3,98,"CALL_VIDEO_PHONE","PUSH_VIDEO_RTP_STREAM"),
	//拨打视频电话
	PLAYBACK(4,0,"PLAYBACK",null),
	// 后续删除
	DOWNLOAD(5,0,"DOWNLOAD",null)
	;

	private final int value;
	private final int pt;
	private final String callName;
	private final String pushName;

	VideoStreamType(int value, int pt,String callName,String pushName) {
		this.value = value;
		this.pt = pt;
		this.callName = callName;
		this.pushName = pushName;
	}

	public static VideoStreamType getCallName(String name){
		return CALL_MAP.get(name);
	}
	public static VideoStreamType getPushName(String name){
		return PUSH_MAP.get(name);
	}

	private static final Map<String, VideoStreamType> CALL_MAP = new HashMap<>();
	private static final Map<String, VideoStreamType> PUSH_MAP = new HashMap<>();
	static {
		for (VideoStreamType s : VideoStreamType.values()) {
			if(StringUtils.isNotEmpty(s.getCallName())){
				CALL_MAP.put(s.getCallName(), s);
			}
			if(StringUtils.isNotEmpty(s.getPushName())){
				PUSH_MAP.put(s.getPushName(), s);
			}
		}
	}
}