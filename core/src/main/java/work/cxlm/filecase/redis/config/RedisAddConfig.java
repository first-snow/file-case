package work.cxlm.filecase.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 需要在 `spring.profiles.configure.includes` 中手动添加后才会生效
 * create 2021/4/15 14:33
 *
 * @author Chiru
 */
@Slf4j
@Configuration
public class RedisAddConfig {

    private RedissonClient redissonClient;

    @Bean
    JsonJacksonCodec jsonJacksonCodec() {
        return new JsonJacksonCodec();
    }
}
