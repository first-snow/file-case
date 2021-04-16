package work.cxlm.filecase.redis.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 需要在 `spring.profiles.configure.includes` 中手动添加后才会生效
 * create 2021/4/15 14:33
 *
 * @author Chiru
 */
@Slf4j
@Configuration
public class RedisAddConfig {

    @Bean
    JsonJacksonCodec jsonJacksonCodec() {
        return new JsonJacksonCodec();
    }
}
