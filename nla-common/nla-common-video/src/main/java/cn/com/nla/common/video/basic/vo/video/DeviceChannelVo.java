package cn.com.nla.common.video.basic.vo.video;

import cn.com.nla.common.video.basic.model.LongIdEntity;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.common.DateFormats;
import cn.com.nla.common.video.basic.enums.GbIdConstant;
import cn.com.nla.common.video.basic.utils.Coordtransform;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
    * 通道信息
    */
@Data
@EqualsAndHashCode(callSuper=true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DeviceChannelVo extends LongIdEntity {
    /**
     * 父级id
     */
    private String parentId;

    /**
     * 通道国标编号
     */
    private String channelId;

    /**
     * 设备国标编号
     */
    private String deviceId;

    /**
     * 通道名
     */
    private String name;

    /**
     * 生产厂商
     */
    private String manufacture;

    /**
     * 型号
     */
    private String model;

    /**
     * 设备归属
     */
    private String owner;

    /**
     * 行政区域
     */
    private String civilCode;

    /**
     * 警区
     */
    private String block;

    /**
     * 安装地址
     */
    private String address;

    /**
     * 是否有子设备 1有, 0没有
     */
    private Integer parental;

    /**
     * 信令安全模式  缺省为0; 0:不采用; 2: S/MIME签名方式; 3: S/ MIME加密签名同时采用方式; 4:数字摘要方式
     */
    private Integer safetyWay;

    /**
     * 注册方式 缺省为1;1:符合IETFRFC3261标准的认证注册模 式; 2:基于口令的双向认证注册模式; 3:基于数字证书的双向认证注册模式
     */
    private Integer registerWay;

    /**
     * 证书序列号
     */
    private String certNum;

    /**
     * 证书有效标识 缺省为0;证书有效标识:0:无效1: 有效
     */
    private Integer certifiable;

    /**
     * 证书无效原因码
     */
    private Integer errCode;

    /**
     * 证书终止有效期
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private LocalDateTime endTime;

    /**
     * 保密属性 缺省为0; 0:不涉密, 1:涉密
     */
    private Integer secrecy;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 密码
     */
    private String password;

    /**
     * 云台类型 0.未知 1.球机 2.半球 3.固定枪机 4.遥控枪机
     */
    private Integer ptzType;

    /**
     * 云台类型描述字符串
     */
    private String ptzTypeText;

    /**
     * 在线/离线， 1在线,0离线
     */
    private Integer status;

    /**
     * 纬度
     */
    private Double longitude = 0.0;

    /**
     * 经度
     */
    private Double latitude = 0.0;

    /**
     * 经度 GCJ02
     */
    private Double longitudeGcj02 = 0.0;

    /**
     * 纬度 GCJ02
     */
    private Double latitudeGcj02 = 0.0;

    /**
     * 经度 WGS84
     */
    private Double longitudeWgs84 = 0.0;

    /**
     * 纬度 WGS84
     */
    private Double latitudeWgs84 = 0.0;

    /**
     * 子设备数
     */
    private Integer subCount;

    /**
     * 流唯一编号，存在表示正在直播
     */
    private String streamId;

    /**
     * 是否开启录像
     */
    private Integer hasRecord;

    /**
     * 是否开启音频
     */
    private Integer hasAudio;
    /**
     * 标记通道的类型，0->国标通道 1->直播流通道 2->业务分组/虚拟组织/行政区划
     */
    private Integer channelType =0;

    /**
     * 业务分组
     */
    private String businessGroupId;

    /**
     * GPS的更新时间
     */

    @JsonFormat(pattern =  DateFormats.DATE_TIME_FORMAT)
    private LocalDateTime gpsTime;


    public DeviceChannelVo initGps(int geoCoordSys){
        if(geoCoordSys == 1 && this.latitude != null && this.longitude != null){
            this.longitudeWgs84 = this.longitude;
            this.latitudeWgs84 = this.latitude;
            Double[] doubles = Coordtransform.WGS84ToGCJ02(this.longitude, this.latitude);
            this.longitudeGcj02 = doubles[0];
            this.latitudeGcj02 = doubles[1];
        }else if(geoCoordSys == 2 && this.latitude != null && this.longitude != null){
            this.longitudeGcj02 = this.longitude;
            this.latitudeGcj02 = this.latitude;
            Double[] doubles = Coordtransform.GCJ02ToWGS84(this.longitude, this.latitude);
            this.longitudeWgs84 = doubles[0];
            this.latitudeWgs84 = doubles[1];
        }else {
            this.latitudeGcj02 = 0.00;
            this.longitudeGcj02 = 0.00;
            this.latitudeWgs84 = 0.00;
            this.longitudeWgs84 = 0.00;
        }
        return this;
    }

    /**
     * 生成国标流通道信息
     * @param platform
     * @param gbStream
     * @param catalog
     * @return
     */
    public static  DeviceChannelVo getDeviceChannelListByGbStream(ParentPlatformVo platform,GbStreamVo gbStream,PlatformCatalogVo catalog) {
        DeviceChannelVo deviceChannel = new DeviceChannelVo();
        deviceChannel.setChannelId(gbStream.getGbId());
        deviceChannel.setName(gbStream.getName());
        deviceChannel.setLongitude(gbStream.getLongitude());
        deviceChannel.setLatitude(gbStream.getLatitude());
        deviceChannel.setDeviceId(platform.getDeviceGbId());
        deviceChannel.setManufacture("springbootcloud");
        deviceChannel.setStatus(ConstEnum.Flag.YES.getValue());
        deviceChannel.setRegisterWay(1);
        deviceChannel.setCivilCode(platform.getAdministrativeDivision());
        if (platform.getTreeType()== GbIdConstant.Type.TYPE_216.getValue()){
            deviceChannel.setCivilCode(gbStream.getCatalogId());
        }else if (platform.getTreeType()==GbIdConstant.Type.TYPE_215.getValue()){
            if (catalog == null) {
                deviceChannel.setParentId(platform.getDeviceGbId());
                deviceChannel.setBusinessGroupId(null);
            }else {
                deviceChannel.setParentId(catalog.getId());
                deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
            }
        }
        deviceChannel.setModel("live");
        deviceChannel.setOwner("springbootcloud");
        deviceChannel.setParental(0);
        deviceChannel.setSecrecy(0);
        return deviceChannel;
    }

    /**
     * 生成目录 通道数据
     * @param platform
     * @param catalog
     * @return
     */
    public static  DeviceChannelVo getDeviceChannelListByPlatformCatalog(ParentPlatformVo platform,PlatformCatalogVo catalog) {
        DeviceChannelVo deviceChannel = new DeviceChannelVo();
        deviceChannel.setChannelId(catalog.getId());
        deviceChannel.setName(catalog.getName());
        deviceChannel.setLongitude(0.0);
        deviceChannel.setLatitude(0.0);
        deviceChannel.setDeviceId(platform.getDeviceGbId());
        deviceChannel.setManufacture("springbootcloud");
        deviceChannel.setStatus(ConstEnum.Flag.YES.getValue());
        deviceChannel.setRegisterWay(1);
        deviceChannel.setCivilCode(platform.getAdministrativeDivision());
       if (platform.getTreeType()==GbIdConstant.Type.TYPE_216.getValue()){
            deviceChannel.setParentId(catalog.getId());
            deviceChannel.setBusinessGroupId(catalog.getBusinessGroupId());
        }
        deviceChannel.setModel("live");
        deviceChannel.setOwner("springbootcloud");
        deviceChannel.setParental(1);
        deviceChannel.setSecrecy(0);
        return deviceChannel;
    }

    public void setPtzType(int ptzType) {
        this.ptzType = ptzType;
        switch (ptzType) {
            case 0:
                this.ptzTypeText = "未知";
                break;
            case 1:
                this.ptzTypeText = "球机";
                break;
            case 2:
                this.ptzTypeText = "半球";
                break;
            case 3:
                this.ptzTypeText = "固定枪机";
                break;
            case 4:
                this.ptzTypeText = "遥控枪机";
                break;
            case 5:
                this.ptzTypeText = "遥控半球";
                break;
            case 6:
                this.ptzTypeText = "多目设备的全景/拼接通道";
                break;
            case 7:
                this.ptzTypeText = "多目设备的分割通道";
                break;
        }
    }
}