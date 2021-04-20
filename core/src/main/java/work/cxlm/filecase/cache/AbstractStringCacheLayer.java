package work.cxlm.filecase.cache;

import work.cxlm.filecase.redis.util.JacksonUtil;

import javax.annotation.Nullable;

/**
 * create 2021/4/16 18:22
 *
 * @author Chiru
 */
public abstract class AbstractStringCacheLayer implements CacheLayer<String, String> {

    @Nullable
    @Override
    public String get(String key) {
        return getInternal(key);
    }

    @Override
    public void put(String key, String value) {
        putInternal(key, value);
    }

    /**
     * 获取缓存
     *
     * @param key  缓存键
     * @param type 值的类型
     * @param <K>  键的类型参数
     * @param <V>  类型参数
     * @return 缓存值
     */
    public <K, V> V getAny(K key, Class<? extends V> type) {
        String keyStr = JacksonUtil.objectToString(key);
        String valueStr = getInternal(keyStr);
        return JacksonUtil.jsonToObject(valueStr, type);
    }

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <K>   键的类型参数
     * @param <V>   值的类型参数
     */
    public <K, V> void putAny(K key, V value) {
        String keyStr = JacksonUtil.objectToString(key);
        String valueStr = JacksonUtil.objectToString(value);
        putInternal(keyStr, valueStr);
    }

    /**
     * put 的缓存层底层实现（API 级别）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    protected abstract void putInternal(String key, String value);

    /**
     * get 到的缓存层底层实现（API 级别）
     *
     * @param key 缓存键
     * @return 缓存值
     */
    protected abstract String getInternal(String key);
}
