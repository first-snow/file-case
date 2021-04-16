package work.cxlm.filecase.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

import java.util.ArrayList;
import java.util.List;

/**
 * Jedis 配置默认不生效，需要在 `spring.profiles.configure.includes` 中手动添加开启
 * 配置类，补充 DsLock 所需 Bean
 *
 * @author Chiru
 */
@Slf4j
@Configuration
public class JedisConfig {

    @Value("${jedis.timeout}")
    private int timeout;

    @Value("${jedis.pool.max-total}")
    private int maxTotal;
    @Value("${jedis.pool.max-wait-millis}")
    private int maxWaitMillis;
    @Value("${jedis.pool.max-idle}")
    private int maxIdle;
    @Value("${jedis.pool.min-idle}")
    private int minIdle;
    @Value("${jedis.pool.min-evictable-idle-time-millis}")
    private long minEvictableIdleTimeMillis;

    @Value("${jedis.master-s1.host}")
    private String masterHost;
    @Value("${jedis.master-s1.port}")
    private int masterPort;
    @Value("${jedis.master-s1.name}")
    private String masterName;
    @Value("${jedis.master-s1.password}")
    private String masterPassword;

    @Value("${jedis.slave-s1.host}")
    private String slaveHost;
    @Value("${jedis.slave-s1.port}")
    private int slavePort;
    @Value("${jedis.slave-s1.name}")
    private String slaveName;
    @Value("${jedis.slave-s1.password}")
    private String slavePassword;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxTotal);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        return jedisPoolConfig;
    }

    @Bean
    public JedisShardInfo masterShard() {
        JedisSharedInfoExtended masterShard = new JedisSharedInfoExtended(masterHost, masterPort, timeout, masterName);
        masterShard.setPassword(masterPassword);
        return masterShard;
    }

    @Bean
    public List<JedisShardInfo> masterShards() {
        List<JedisShardInfo> masterShards = new ArrayList<>();
        addShard(masterShards, masterHost, masterPort, timeout, masterName, masterPassword);
        return masterShards;
    }

    @Bean
    public List<JedisShardInfo> slaveShards() {
        List<JedisShardInfo> slaveShards = new ArrayList<>();
        addShard(slaveShards, slaveHost, slavePort, timeout, slaveName, slavePassword);
        return slaveShards;
    }

    private void addShard(List<JedisShardInfo> shards, String host, int port, int timeout, String name,
        String password) {
        JedisSharedInfoExtended shard = new JedisSharedInfoExtended(host, port, timeout, name);
        shard.setPassword(password);
        shards.add(shard);
    }

    @Bean(name = "masterJedisPool")
    public ShardedJedisPool masterJedisPool(JedisPoolConfig jedisPoolConfig, List<JedisShardInfo> masterShards) {
        return new ShardedJedisPool(jedisPoolConfig, masterShards);
    }

    @Bean(name = "slaveJedisPool")
    public ShardedJedisPool slaveJedisPool(JedisPoolConfig jedisPoolConfig, List<JedisShardInfo> slaveShards) {
        return new ShardedJedisPool(jedisPoolConfig, slaveShards);
    }

    private static class JedisSharedInfoExtended extends JedisShardInfo {
        public JedisSharedInfoExtended(String host, int port, int timeout, String name) {
            super(host, port, timeout, name);
        }

        @Override
        public void setPassword(String auth) {
            if (auth != null && auth.trim().length() > 0) {
                super.setPassword(auth);
            }
        }
    }
}
