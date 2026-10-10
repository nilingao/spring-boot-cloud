package cn.com.nla.common.video.basic.model;

import java.util.LinkedHashMap;

/** ZLM query and hook fields with explicit null defaults.
 * @author TZY
 */
public class NotNullMap extends LinkedHashMap<String, Object> {
    public void putString(String key, String value) { putString(key, value, ""); }
    public void putString(String key, String value, String fallback) { put(key, value == null ? fallback : value); }
    public void putInteger(String key, Integer value) { putInteger(key, value, 0); }
    public void putInteger(String key, Integer value, int fallback) { put(key, value == null ? fallback : value); }
    public void putLong(String key, Long value) { putLong(key, value, 0L); }
    public void putLong(String key, Long value, long fallback) { put(key, value == null ? fallback : value); }
    public void putDouble(String key, Double value) { putDouble(key, value, 0D); }
    public void putDouble(String key, Double value, double fallback) { put(key, value == null ? fallback : value); }
}
