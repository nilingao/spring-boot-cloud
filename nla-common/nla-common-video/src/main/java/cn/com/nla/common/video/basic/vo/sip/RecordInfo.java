package cn.com.nla.common.video.basic.vo.sip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @description:设备录像信息bean
 * @author TZY
 * @date:   2020年5月8日 下午2:05:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordInfo implements Serializable {

	private String deviceId;

	private String channelId;

	private String sn;

	private String name;

	private int sumNum;

	private int count;

	private List<RecordItem> recordList;

}
