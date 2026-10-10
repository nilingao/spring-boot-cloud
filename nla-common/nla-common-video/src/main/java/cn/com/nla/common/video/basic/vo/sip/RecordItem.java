package cn.com.nla.common.video.basic.vo.sip;


import cn.hutool.core.date.DateUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备录像bean
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordItem implements Comparable<RecordItem>{

	private String deviceId;

	private String name;

	private String filePath;

	private String fileSize;

	private String address;

	private String startTime;

	private String endTime;

	private int secrecy;

	private String type;

	private String recorderId;

	@Override
	public int compareTo( RecordItem recordItem) {
		return DateUtil.parse(startTime).compareTo(DateUtil.parse(recordItem.getStartTime()));
	}
}
