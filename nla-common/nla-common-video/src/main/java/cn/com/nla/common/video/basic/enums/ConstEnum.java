package cn.com.nla.common.video.basic.enums;

/**
 * Video 封装内部通用枚举集合。
 * <p>
 * 迁移自旧 springbootcomm ConstEnum，仅保留视频封装实际使用的
 * {@link Flag}（是/否标记）与 {@link ContentType}（内容类型）。
 *
 * @author TZY
 */
public interface ConstEnum {

    /**
     * 是否标记
     */
    enum Flag {
        /**
         * 否
         */
        NO(0, "否"),
        /**
         * 是
         */
        YES(1, "是"),
        ;

        private final int value;
        private final String name;

        Flag(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 内容类型
     */
    enum ContentType {
        /**
         * JSON
         */
        JSON("application/json;charset=UTF-8");

        private final String value;

        ContentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
