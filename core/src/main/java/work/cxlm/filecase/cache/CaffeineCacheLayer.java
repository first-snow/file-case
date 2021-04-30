package work.cxlm.filecase.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * create 2021/4/27 18:29
 *
 * @author Chiru
 */
public class CaffeineCacheLayer extends AbstractStringCacheLayer{

    private static final Cache<@NonNull String, @NonNull Object> CAFFEINE_CACHE =
            Caffeine.newBuilder()
                    //设置cache的初始大小为10，要合理设置该值
                    .initialCapacity(10)
                    // 最大值
                    .maximumSize(10000)
                    //设置cache中的数据在写入之后的存活时间为1小时
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    //构建cache实例
                    .build();
    @Override
    protected void putInternal(String key, String value) {
        CAFFEINE_CACHE.put(key, value);
    }

    @Override
    protected String getInternal(String key) {
        Object valueObj = CAFFEINE_CACHE.getIfPresent(key);
        if (null != valueObj) {
            return valueObj.toString();
        }
        return null;
    }

    @Override
    public void delete(String key) {
        CAFFEINE_CACHE.invalidate(key);
    }

    @Override
    public void clear() {
        CAFFEINE_CACHE.invalidateAll();
    }

    @Override
    public String getLayerName() {
        return "caffeine";
    }
}
