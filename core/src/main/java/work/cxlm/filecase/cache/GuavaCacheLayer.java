package work.cxlm.filecase.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 * create 2021/4/16 19:35
 *
 * @author Chiru
 */
public class GuavaCacheLayer extends AbstractStringCacheLayer{

    private static final Cache<String, Object> GUAVA_CACHE =
            CacheBuilder.newBuilder()
                    //设置cache的初始大小为10，要合理设置该值
                    .initialCapacity(10)
                    // 最大值
                    .maximumSize(10000)
                    //设置并发数为5，即同一时间最多只能有5个线程往cache执行写入操作
                    .concurrencyLevel(5)
                    //设置cache中的数据在写入之后的存活时间为1小时
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    //构建cache实例
                    .build();
    @Override
    protected void putInternal(String key, String value) {
        GUAVA_CACHE.put(key, value);
    }

    @Override
    protected String getInternal(String key) {
        Object valueObj = GUAVA_CACHE.getIfPresent(key);
        if (null != valueObj) {
            return valueObj.toString();
        }
        return null;
    }

    @Override
    public void delete(String key) {
        GUAVA_CACHE.invalidate(key);
    }

    @Override
    public void clear() {
        GUAVA_CACHE.invalidateAll();
    }

    @Override
    public String getLayerName() {
        return "guava";
    }
}
