package work.cxlm.filecase.cache;

import javax.annotation.Nullable;

/**
 * 缓存层，用在缓存管理中心
 * create 2021/4/16 18:04
 *
 * @author Chiru
 */
public interface CacheLayer<K, V> {

    /**
     * 从缓存中读取值
     *
     * @param key 缓存键
     * @return 对应的值，不存在时，返回 null
     */
    @Nullable
    V get(K key);

    /**
     * 像缓存中添加缓存项
     *
     * @param key   键
     * @param value 值
     */
    void put(K key, V value);

    /**
     * 删除指定的缓存
     *
     * @param key 指定的缓存键
     */
    void delete(K key);

    /**
     * 清除全部缓存
     */
    void clear();

    /**
     * 获取本层缓存底层实现的中间件名
     *
     * @return 如：redis, guava 等
     */
    String getLayerName();
}
