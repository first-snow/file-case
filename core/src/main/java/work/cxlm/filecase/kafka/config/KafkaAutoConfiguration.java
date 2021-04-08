package work.cxlm.filecase.kafka.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import work.cxlm.filecase.kafka.properties.CustomerKafkaProperties;

/**
 * create 2021/4/7 14:36
 *
 * @author Chiru
 */
@Configuration
@EnableConfigurationProperties(CustomerKafkaProperties.class)
public class KafkaAutoConfiguration implements ApplicationContextAware, InitializingBean, BeanPostProcessor {

    private ApplicationContext context;
    private final CustomerKafkaProperties properties;
    @Value("${spring.application.name}")
    private String applicationName;

    public KafkaAutoConfiguration(CustomerKafkaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        if (context == null) {
            context = applicationContext;
        }
    }

    @Override
    public void afterPropertiesSet() {
        KafkaContainerFactory kafkaContainerFactory = new KafkaContainerFactory(
                properties, context);
        kafkaContainerFactory.setApplicationName(this.applicationName);
        kafkaContainerFactory.buildContainerFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    @Order
    public CustomerKafkaTemplate<String, String> customerKafkaTemplate(CustomerKafkaProperties properties) {
        return new CustomerKafkaTemplate<>(properties);
    }
}
