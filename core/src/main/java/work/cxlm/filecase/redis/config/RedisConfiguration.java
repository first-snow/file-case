package work.cxlm.filecase.redis.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.redis.properties.RedisProperties;
import work.cxlm.filecase.redis.redisson.RedissonClientFactory;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * REDIS 自动配置
 * create 2021/4/11 15:16
 *
 * @author Chiru
 */
@Slf4j
@Configuration
@AutoConfigureBefore(name = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.JedisConnectionConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration"
})
@EnableConfigurationProperties(RedisProperties.class)
@Import(RedisConfiguration.RedisClientRegistrar.class)
public class RedisConfiguration {

    private static final String DEFAULT_REDIS_NAME = "default";
    private static final String REDIS_CLIENT_PREFIX = "redis.clients";
    private static final String REDISSON_NAME = "redissonClient";
    private static final String REDISSON_NAME_SUFFIX = "RedissonClient";

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redissonConnectionFactory(RedissonClient defaultClient) {
        return new RedissonConnectionFactory(defaultClient);
    }

    /**
     * 正规化数据源名称，以 RedissonClient 为后缀的
     *
     * @param name 数据源名称
     * @return 数据源名称
     */
    private static String normalizeDataSourceName(String name) {
        if (StringUtils.isEmpty(name) || REDISSON_NAME.equals(name)) {
            return REDISSON_NAME;
        }
        if (name.endsWith(REDISSON_NAME_SUFFIX)) {
            return name;
        }
        return name + REDISSON_NAME_SUFFIX;
    }

    /**
     * 注册客户端
     *
     * @author trang
     */
    static class RedisClientRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

        private Map<String, Object> clients;

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            this.clients = Binder.get(environment)
                    .bind(REDIS_CLIENT_PREFIX, Bindable.mapOf(String.class, Object.class))
                    .orElse(emptyMap());
        }

        @Override
        public void registerBeanDefinitions(@NonNull AnnotationMetadata annotationMetadata, @NonNull BeanDefinitionRegistry registry) {
            clients.keySet().forEach(key -> {
                // 注册客户端工厂Bean
                val clientBeanName = normalizeDataSourceName(key);
                val factoryBeanName = clientBeanName + "Factory";
                val factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedissonClientFactory.class)
                        .addConstructorArgValue(key);
                registry.registerBeanDefinition(factoryBeanName, factoryBuilder.getBeanDefinition());

                // 解析参数，识别并选择合适的客户端类型
                Class<?> clientClass = RedissonClient.class;
                String factoryMethodName = "createClient";
                if (clients.get(key) instanceof Map) {
                    val props = (Map<?, ?>) clients.get(key);
                    if (props.containsKey("type") && "REACTIVE".equalsIgnoreCase(props.get("type").toString())) {
                        clientClass = RedissonReactiveClient.class;
                        factoryMethodName = "createRedissonReactiveClient";
                    } else if (props.containsKey("type") && "RX".equalsIgnoreCase(props.get("type").toString())) {
                        clientClass = RedissonRxClient.class;
                        factoryMethodName = "RX";
                    }
                }

                // 注册客户端Bean
                val clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(clientClass)
                        .setFactoryMethodOnBean(factoryMethodName, factoryBeanName)
                        .setDestroyMethodName("shutdown");
                registry.registerBeanDefinition(clientBeanName, clientBuilder.getBeanDefinition());
                if (DEFAULT_REDIS_NAME.equals(key)) {
                    registry.registerAlias(clientBeanName, REDISSON_NAME);
                }

                log.info("加载 Redisson 客户端 Bean: {}", clientBeanName);
            });
        }
    }
}
