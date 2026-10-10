package cn.com.nla.common.video.basic.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * 国标编码规范生成器
 * 国标编码规范
 * 1.2位 省级编码
 * 3.4位 市级编码
 * 5.6位 区级编码
 * 7.8 基层接入单位编号
 * 9.10 行业编码
 * 11.12.13 类型编码
 * 14 网络标识编码
 * 15~20 设备 用户序号
 */
public class GbIdConstant {

    /**
     * 省级编码
     */
    public static enum Provincial {
        UTF_8(1, "UTF-8"),
        GB2312(2, "GB2312"),
        ;
        private final int value;
        private final String name;

        Provincial(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<Integer , String> map = new HashMap<Integer , String>();
        static {
            for (Provincial s : Provincial.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

    /**
     * 市级编码
     */
    public static enum Municipal {
        UTF_8(1, "UTF-8"),
        GB2312(2, "GB2312"),
        ;
        private final int value;
        private final String name;

        Municipal(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<Integer , String> map = new HashMap<Integer , String>();
        static {
            for (Municipal s : Municipal.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

    /**
     * 区级编码
     */
    public static enum District {
        UTF_8(1, "UTF-8"),
        GB2312(2, "GB2312"),
        ;
        private final int value;
        private final String name;

        District(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<Integer , String> map = new HashMap<Integer , String>();
        static {
            for (District s : District.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }


    /**
     * 基层接入单位编号
     */
    public static enum Bottom {
        UTF_8(1, "UTF-8"),
        GB2312(2, "GB2312"),
        ;
        private final int value;
        private final String name;

        Bottom(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<Integer , String> map = new HashMap<Integer , String>();
        static {
            for (Bottom s : Bottom.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

    /**
     * 行业编码
     * 00-39 政府机关
     * 40-79 企业/事业单位
     * 80-89 居民自建
     * 90-99 其他主体
     */
    public static enum Industry {
        INDUSTRY_0("00", "社会治安路面接入"),
        INDUSTRY_1("01", "社会治安社区接入"),
        INDUSTRY_2("02", "社会治安内部接入"),
        INDUSTRY_3("03", "社会治安其他接入"),
        INDUSTRY_4("04", "交通路面接入"),
        INDUSTRY_5("05", "交通卡口接入"),
        INDUSTRY_6("06", "交通内部接入"),
        INDUSTRY_7("07", "交通其他接入"),
        INDUSTRY_8("08", "城市管理接入"),
        INDUSTRY_9("09", "卫生环保接入"),
        INDUSTRY_10("10", "商检海关接入"),
        INDUSTRY_11("11", "教育部门接入"),
        INDUSTRY_39("39", "政府机关预留"),//12-39 政府机关预留
        INDUSTRY_40("40", "农林牧渔业接入"),
        INDUSTRY_41("41", "采矿企业接入"),
        INDUSTRY_42("42", "制造企业接入"),
        INDUSTRY_43("43", "冶金企业接入"),
        INDUSTRY_44("44", "电力企业接入"),
        INDUSTRY_45("45", "燃气企业接入"),
        INDUSTRY_46("46", "建筑企业接入"),
        INDUSTRY_47("47", "物流企业接入"),
        INDUSTRY_48("48", "邮政企业接入"),
        INDUSTRY_49("49", "信息企业接入"),
        INDUSTRY_50("50", "住宿和餐饮业接入"),
        INDUSTRY_51("51", "金融企业接入"),
        INDUSTRY_52("52", "房地产业接入"),
        INDUSTRY_53("53", "商务服务业接入"),
        INDUSTRY_54("54", "水利企业接入"),
        INDUSTRY_55("55", "娱乐企业接入"),
        INDUSTRY_79("79", "企业/事业单位预留"),//56-79 企业/事业单位预留
        INDUSTRY_89("89", "居民自建预留"),//80-89 居民自建预留
        INDUSTRY_99("99", "其他主体预留"),//90-99 其他主体预留

        ;
        private final String value;
        private final String name;

        Industry(String value, String name) {
            this.value = value;
            this.name = name;
        }
        public String getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<String , String> map = new HashMap<String , String>();
        static {
            for (Industry s : Industry.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }

    /**
     * 类型编码
     * 111-130  表示类型为前端主设备
     * 131-199  表示类型为前端外围设备
     * 200-299  表示类型为平台设备
     * 300-399  表示类型为中心用户
     * 400-499  表示类型为终端用户
     * 500-599  表示类型为为平台外接服务器
     * 600-999  扩展类型
     */
    public static enum Type {
        TYPE_111(111, "DVR编码"),
        TYPE_112(112, "视频服务器编码"),
        TYPE_113(113, "编码器编码"),
        TYPE_114(114, "解码器编码"),
        TYPE_115(115, "视频切换矩阵编码"),
        TYPE_116(116, "音频切换矩阵编码"),
        TYPE_117(117, "报警控制器编码"),
        TYPE_118(118, "网络视频录像机(NVR)编码"),
        TYPE_130(130, "混合硬盘录像机(HVR)编码"),
        TYPE_119(119, "扩展的前端主设备类型"),//119-130都是扩展的前端主设备类型
        TYPE_131(131, "摄像机编码"),
        TYPE_132(132, "网络摄像机(IPC)编码"),
        TYPE_133(133, "显示器编码"),
        TYPE_134(134, "报警输入设备编码(如红外、烟感、门禁等报警设备)"),
        TYPE_135(135, "报警输出设备编码(如警灯、警铃等设备)"),
        TYPE_136(136, "语音输入设备编码"),
        TYPE_137(137, "语音输出设备"),
        TYPE_138(138, "移动传输设备编码"),
        TYPE_139(139, "其他外围设备编码"),
        TYPE_199(199, "扩展的前端外围设备类型"),//140~199 扩展的前端外围设备类型
        TYPE_200(200, "中心信令控制服务器编码"),
        TYPE_201(201, "Web应用服务器编码"),
        TYPE_202(202, "媒体分发服务器编码"),
        TYPE_203(203, "代理服务器编码"),
        TYPE_204(204, "安全服务器编码"),
        TYPE_205(205, "报警服务器编码"),
        TYPE_206(206, "数据库服务器编码"),
        TYPE_207(207, "GIS服务器编码"),
        TYPE_208(208, "管理服务器编码"),
        TYPE_209(209, "接入网关编码"),
        TYPE_210(210, "媒体存储服务器编码"),
        TYPE_211(211, "信令安全路由网关编码"),
        TYPE_215(215, "业务分组编码"),
        TYPE_216(216, "虚拟组织编码"),
        TYPE_299(299, "扩展的平台设备类型"),//212-214 217-299 扩展的平台设备类型
        TYPE_300(300, "中心用户"),
        TYPE_343(343, "行业角色用户"),//301-343 行业角色用户
        TYPE_399(399, "行业角色用户"),//344-399 扩展的中心用户类型
        TYPE_400(400, "终端用户"),
        TYPE_443(443, "行业角色用户"),//401-443 行业角色用户
        TYPE_499(499, "扩展的终端用户类型"),//344-399 扩展的终端用户类型
        TYPE_500(500, "视频图像信息综合应用平台信令服务器"),
        TYPE_501(501, "视频图像信息运维管理平台信令服务器"),
        TYPE_599(599, "扩展的平台外接服务器类型"),//502-599 扩展的平台外接服务器类型
        TYPE_999(999, "扩展类型"),//600-999 扩展类型
        ;
        private final int value;
        private final String name;

        Type(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static Type getType(Integer value) {return map.get(value);}
        private static Map<Integer , Type> map = new HashMap<Integer , Type>();
        static {
            for (Type s : Type.values()) {
                map.put(s.getValue(), s);
            }
        }
    }

    /**
     * 类型编码
     * 0.1.2.3.4 为监控报警专网
     * 5.为公安信息网
     * 6.为政务网
     * 7.为Internet网
     * 8.为社会资源接入网
     * 9.预留
     */
    public static enum Network {
        NETWORK_0(0, "监控报警专网"),
        NETWORK_1(1, "监控报警专网"),
        NETWORK_2(2, "监控报警专网"),
        NETWORK_3(3, "监控报警专网"),
        NETWORK_4(4, "监控报警专网"),
        NETWORK_5(5, "公安信息网"),
        NETWORK_6(6, "政务网"),
        NETWORK_7(7, "Internet网"),
        NETWORK_8(8, "社会资源接入网"),
        NETWORK_9(9, "预留"),
        ;
        private final int value;
        private final String name;

        Network(int value, String name) {
            this.value = value;
            this.name = name;
        }
        public int getValue() {return value;}
        public String getName() {return name;}
        public static String getName(Integer value) {return map.get(value);}
        private static Map<Integer , String> map = new HashMap<Integer , String>();
        static {
            for (Network s : Network.values()) {
                map.put(s.getValue(), s.getName());
            }
        }
    }
}
