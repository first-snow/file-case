package work.cxlm.filecase.redis.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.redis.properties.RedisProperties;
import work.cxlm.filecase.redis.redisson.RedissonClientFactory;
import work.cxlm.filecase.util.SpringContextUtils;

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

    private static final String REDIS_CLIENT_PREFIX = "redis.clients";
    private static final String DEFAULT_REDIS_PREFIX = "redis.default-name";
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
    private static String ensureRedissonClientSuffix(String name) {
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
        /**
         * 默认使用的 Redis 名称，如果指定了默认名称则会被覆盖
         */
        private String defaultNodeName = null;

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            this.clients = Binder.get(environment)
                    .bind(REDIS_CLIENT_PREFIX, Bindable.mapOf(String.class, Object.class))
                    .orElse(emptyMap());
            this.defaultNodeName = Binder.get(environment).bind(DEFAULT_REDIS_PREFIX, Bindable.of(String.class)).orElse("default");
        }

        @Override
        public void registerBeanDefinitions(@NonNull AnnotationMetadata annotationMetadata, @NonNull BeanDefinitionRegistry registry) {
            clients.keySet().forEach(key -> {
                String clientFactoryBeanName = registerRedissonClientFactory(registry, key);
                String clientBeanName = registerRedissonClient(registry, key, clientFactoryBeanName);

                String connectionFactoryBeanName = registerConnectionFactory(registry, key, clientBeanName);
                registerRedisTemplate(registry, key, connectionFactoryBeanName);
            });
        }

        private void registerRedisTemplate(BeanDefinitionRegistry registry, String key, String connectionFactoryBeanName) {
            String stringRedisTemplateBeanName = key + "StringRedisTemplate";
            String redisTemplateBeanName = key + "RedisTemplate";

            val stringTemplateBuilder = BeanDefinitionBuilder.genericBeanDefinition(StringRedisTemplate.class, () -> {
                RedissonConnectionFactory redissonConnectionFactory = (RedissonConnectionFactory) SpringContextUtils.getBean(connectionFactoryBeanName);
                return new StringRedisTemplate(redissonConnectionFactory);
            });
            val templateBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedisTemplate.class, () -> {
                RedissonConnectionFactory redissonConnectionFactory = (RedissonConnectionFactory) SpringContextUtils.getBean(connectionFactoryBeanName);
                RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
                redisTemplate.setConnectionFactory(redissonConnectionFactory);
                redisTemplate.afterPropertiesSet();
                return redisTemplate;
            });
            registry.registerBeanDefinition(stringRedisTemplateBeanName, stringTemplateBuilder.getBeanDefinition());
            registry.registerBeanDefinition(redisTemplateBeanName, templateBuilder.getBeanDefinition());

            if (defaultNodeName.equals(key)) {
                registry.registerAlias(stringRedisTemplateBeanName, "stringRedisTemplate");
                registry.registerAlias(redisTemplateBeanName, "redisTemplate");
            }

            log.info("注册 StringRedisTemplateFactory Bean: {}", stringRedisTemplateBeanName);
            log.info("注册 RedisTemplateFactory Bean: {}", redisTemplateBeanName);
        }

        private String registerConnectionFactory(BeanDefinitionRegistry registry, String key, String clientBeanName) {
            String connectionFactoryBeanName = key + "RedissonConnectionFactory";

            val factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedissonConnectionFactory.class, () -> {
                RedissonClient clientBean = (RedissonClient) SpringContextUtils.getBean(clientBeanName);
                return new RedissonConnectionFactory(clientBean);
            });
            registry.registerBeanDefinition(connectionFactoryBeanName, factoryBuilder.getBeanDefinition());

            if (defaultNodeName.equals(key)) {
                registry.registerAlias(connectionFactoryBeanName, "redissonConnectionFactory");
            }

            log.info("注册 RedissonConnectionFactory Bean: {}", connectionFactoryBeanName);
            return connectionFactoryBeanName;
        }

        private String registerRedissonClientFactory(BeanDefinitionRegistry registry, String key) {
            // 注册客户端工厂Bean
            val clientBeanName = ensureRedissonClientSuffix(key);
            val factoryBeanName = clientBeanName + "Factory";
            val factoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(RedissonClientFactory.class)
                    .addConstructorArgValue(key);
            registry.registerBeanDefinition(factoryBeanName, factoryBuilder.getBeanDefinition());
            log.info("注册 Bean: {}", factoryBeanName);
            return factoryBeanName;
        }

        private String registerRedissonClient(BeanDefinitionRegistry registry, String key, String factoryBeanName) {
            String clientTypeKey = "type";
            val clientBeanName = ensureRedissonClientSuffix(key);
            // 解析参数，识别并选择合适的客户端类型
            Class<?> clientClass = RedissonClient.class;
            String factoryMethodName = "createClient";
            if (clients.get(key) instanceof Map) {
                val props = (Map<?, ?>) clients.get(key);
                if (props.containsKey(clientTypeKey) && "REACTIVE".equalsIgnoreCase(props.get(clientTypeKey).toString())) {
                    clientClass = RedissonReactiveClient.class;
                    factoryMethodName = "createRedissonReactiveClient";
                } else if (props.containsKey(clientTypeKey) && "RX".equalsIgnoreCase(props.get(clientTypeKey).toString())) {
                    clientClass = RedissonRxClient.class;
                    factoryMethodName = "RX";
                }
            }

            // 注册客户端 Bean (Builder)
            val clientBuilder = BeanDefinitionBuilder.genericBeanDefinition(clientClass)
                    .setFactoryMethodOnBean(factoryMethodName, factoryBeanName)
                    .setDestroyMethodName("shutdown");
            registry.registerBeanDefinition(clientBeanName, clientBuilder.getBeanDefinition());
            if (defaultNodeName.equals(key)) {
                registry.registerAlias(clientBeanName, REDISSON_NAME);
            }

            log.info("注册 RedissonClient Bean: {}", clientBeanName);
            return clientBeanName;
        }
    }
}
