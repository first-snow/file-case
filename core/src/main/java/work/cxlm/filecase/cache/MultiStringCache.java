package work.cxlm.filecase.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import work.cxlm.filecase.exception.CacheNotExistException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * create 2021/4/16 18:03
 *
 * @author Chiru
 */
@Slf4j
public class MultiStringCache {

    private final List<AbstractStringCacheLayer> cacheLayerList;
    private final int layerCount;

    public MultiStringCache(List<AbstractStringCacheLayer> cacheLayerList) {
        this.cacheLayerList = cacheLayerList;
        layerCount = cacheLayerList.size();
        if (0 == layerCount) {
            throw new CacheNotExistException("没有指定缓存层");
        }
    }

    /**
     * 从各层缓存中获取缓存值，不存在的时候返回默认值
     *
     * @param key          缓存键
     * @param defaultValue 默认值
     * @return 遍历各层缓存，得到值则返回，否则返回默认值
     */
    public String getCache(@NonNull String key, @Nullable String defaultValue) {
        String cacheRes = iterateFromUpToDown(layer -> layer.get(key));
        if (null == cacheRes) {
            return defaultValue;
        }
        return cacheRes;
    }

    /**
     * 从各层缓存中获取缓存值，不存在的时候返回 null
     *
     * @param key 缓存键
     * @return 遍历各层缓存，得到值则返回，否则返回 null
     */
    public String getCache(@NonNull String key) {
        return getCache(key, null);
    }

    /**
     * 从指定缓存层获取缓存
     *
     * @param key        缓存键
     * @param layerIndex 指定缓存层的索引
     * @return 指定层指定键的缓存值，没有则返回 null
     */
    public String getFrom(@NonNull String key, int layerIndex) {
        return cacheLayerList.get(layerIndex).get(key);
    }

    /**
     * 将缓存设置到所有层
     *
     * @param key   键
     * @param value 值
     */
    public void set(@NonNull String key, @Nullable String value) {
        iterateFromDownToUp(layer -> layer.put(key, value));
    }

    /**
     * 设置缓存，值为可以转化为 json 的任意类型
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void setAny(@NonNull String key, @Nullable Object value) {
        iterateFromDownToUp(layer -> layer.putAny(key, value));
    }

    /**
     * 获取缓存，值为 V 指定的类型
     *
     * @param key        缓存键
     * @param valueClass 缓存值的 Class
     * @param <V>        缓存值的类型参数
     * @return 得到的缓存值，没有则返回 null
     */
    public <V> V getAnyCache(@NonNull String key, @NonNull Class<V> valueClass) {
        return iterateFromUpToDown(layer -> layer.getAny(key, valueClass));
    }

    /**
     * 获取缓存，值为 V 指定的类型
     *
     * @param key        缓存键
     * @param valueClass 缓存值的 Class
     * @param <V>        缓存值的类型参数
     * @param supplier   缓存不存在时，调用该方法进行查询，并写入各层缓存
     * @return 得到的缓存值，没有则返回 supplier 的结果
     */
    public <V> V getAny(@NonNull String key, @NonNull Class<V> valueClass, Supplier<V> supplier) {
        V cachedValue = getAnyCache(key, valueClass);
        if (null == cachedValue) {
            cachedValue = supplier.get();
            setAny(key, cachedValue);
        }
        return cachedValue;
    }

    /**
     * 将缓存设置到指定层，不对其它层产生影响
     *
     * @param key        缓存键
     * @param value      值
     * @param layerIndex 要设置的层编号
     */
    public void set(@NonNull String key, @Nullable String value, int layerIndex) {
        cacheLayerList.get(layerIndex).put(key, value);
    }

    /**
     * 获取缓存，如果没有则使用 supplier 查询，并逐层写入缓存
     *
     * @param key      缓存键
     * @param supplier 缓存中没有值时，调用的查询函数
     * @return 得到的值，缓存中存在则为缓存中的值，否则为 supplier 中的值
     */
    public String get(@NonNull String key, Supplier<String> supplier) {
        String gotValue = getCache(key);
        if (null == gotValue) {
            gotValue = supplier.get();
        }
        set(key, gotValue);
        return gotValue;
    }

    /**
     * 清除指定层以上的全部缓存
     *
     * @param layerIndex 开始清除的层（包含）
     */
    public void clearLayer(int layerIndex) {
        for (; layerIndex < layerCount; layerIndex++) {
            cacheLayerList.get(layerIndex).clear();
        }
    }

    /**
     * 清除全部缓存
     */
    public void clearAll() {
        clearLayer(0);
    }

    /**
     * 删除某条缓存
     *
     * @param key 缓存键
     */
    public void delete(@NonNull String key) {
        for (AbstractStringCacheLayer kvCacheLayer : cacheLayerList) {
            kvCacheLayer.delete(key);
        }
    }

    private void iterateFromDownToUp(Consumer<AbstractStringCacheLayer> function) {
        for (int i = 0; i < layerCount; i++) {
            function.accept(cacheLayerList.get(i));
        }
    }

    private <T> T iterateFromUpToDown(Function<AbstractStringCacheLayer, T> function) {
        for (int i = layerCount - 1; i >= 0; i--) {
            T apply = function.apply(cacheLayerList.get(i));
            if (null != apply) {
                return apply;
            }
        }
        return null;
    }
}
