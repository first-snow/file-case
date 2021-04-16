package work.cxlm.filecase.redis.redisson;

import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import work.cxlm.filecase.redis.properties.RedisProperties;

/**
 * create 2021/4/11 16:22
 *
 * @author Chiru
 */
public class RedissonClientFactory {

    private final String key;
    private final RedisProperties properties;
    private final ApplicationContext applicationContext;

    public RedissonClientFactory(String key, @Autowired RedisProperties properties, @Autowired ApplicationContext context) {
        this.key = key;
        this.properties = properties;
        this.applicationContext = context;
    }

    public RedissonClient createClient() {
        Assert.isTrue(properties.getClients().containsKey(key), "配置不存在: " + key);
        return new RedissonClientBuilder(key, properties.getClients().get(key), applicationContext).build();
    }

    public RedissonReactiveClient createRedissonReactiveClient() {
        Assert.isTrue(properties.getClients().containsKey(key), "配置不存在: " + key);
        return new RedissonClientBuilder(key, properties.getClients().get(key), applicationContext).buildReactiveClient();
    }

    public RedissonRxClient createRedissonRxClient() {
        Assert.isTrue(properties.getClients().containsKey(key), "配置不存在: " + key);
        return new RedissonClientBuilder(key, properties.getClients().get(key), applicationContext).buildRxClient();
    }
}
