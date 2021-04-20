package work.cxlm.filecase.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;

/**
 * create 2021/4/16 19:36
 *
 * @author Chiru
 */
public class RedisCacheLayer extends AbstractStringCacheLayer {

    private final RedisTemplate<String, String> template;

    public RedisCacheLayer(RedisTemplate<String, String> template) {
        this.template = template;
    }

    @Override
    protected void putInternal(String key, String value) {
        ValueOperations<String, String> redisOperation = template.opsForValue();
        redisOperation.set(key, value);
    }

    @Override
    protected String getInternal(String key) {
        ValueOperations<String, String> redisOperation = template.opsForValue();
        return redisOperation.get(key);
    }

    @Override
    public void delete(String key) {
        template.delete(key);
    }

    @Override
    public void clear() {
        Set<String> allKeys = template.keys("*");
        if (null != allKeys) {
            template.delete(allKeys);
        }
    }

    @Override
    public String getLayerName() {
        return "redis";
    }
}
