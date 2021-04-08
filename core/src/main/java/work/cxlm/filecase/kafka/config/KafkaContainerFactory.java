package work.cxlm.filecase.kafka.config;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.kafka.properties.CustomerKafkaProperties;
import work.cxlm.filecase.kafka.utils.KafkaPropertiesReader;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 容器工厂
 * create 2021/4/7 14:44
 *
 * @author Chiru
 */
@Slf4j
public class KafkaContainerFactory {

    private final CustomerKafkaProperties properties;
    private final ApplicationContext applicationContext;

    @Setter
    private String applicationName;

    public KafkaContainerFactory(CustomerKafkaProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * 本类唯一开放核心方法，构建容器工厂
     */
    public void buildContainerFactory() {
        if (null == properties || properties.getConfig() == null) {
            return;
        }
        boolean defaultFactory = false;
        for (Map.Entry<String, KafkaProperties> kafkaPropertiesEntry : properties.getConfig().entrySet()) {
            KafkaProperties properties = kafkaPropertiesEntry.getValue();
            if (KafkaPropertiesReader.bootStrapServersNotPresent(properties)) {
                // 未指定集群时直接跳过
                continue;
            }
            KafkaProperties.Consumer consumer = properties.getConsumer();
            // 解析 Listener names 并分割、存储到 listenerNames 集合中
            Set<String> listenerNames = null;
            String listenerNamesStr;
            if (consumer != null && !StringUtils.isEmpty(
                    listenerNamesStr = consumer.getProperties().get(KafkaConst.LISTENER_NAMES_PROP))) {
                listenerNames = new LinkedHashSet<>(Arrays.asList(listenerNamesStr.split(",")));
            }

            if (CollectionUtils.isEmpty(listenerNames)) {
                log.info("单独 listener group");
                properties.getConsumer().setGroupId(KafkaPropertiesReader.getGroupId(properties,"default-", applicationName));
                AbstractBeanDefinition abstractBeanDefinition = initContainerFactory(kafkaPropertiesEntry, defaultFactory);
                if (applicationContext.containsBean(KafkaConst.DEFAULT_CONSUMER_FACTORY_BEAN_NAME)) {
                    defaultFactory = true;
                }
                if (!defaultFactory) {
                    getBeanFactory().registerBeanDefinition(KafkaConst.DEFAULT_CONSUMER_FACTORY_BEAN_NAME, abstractBeanDefinition);
                    defaultFactory = true;
                }
            } else {
                log.info("多 listener group: {}", listenerNames);
                for (String listenerName : listenerNames) {
                    properties.getConsumer().setGroupId(KafkaPropertiesReader.getGroupId(properties, listenerName, applicationName));
                    initContainerFactory(kafkaPropertiesEntry, defaultFactory);
                }
            }
        }
    }

    // ============= PRIVATE METHODS ==================

    @SuppressWarnings("rawtypes")
    private AbstractBeanDefinition initContainerFactory(Map.Entry<String, KafkaProperties> entry, boolean defaultFactory) {
        KafkaProperties kafkaProperties = entry.getValue();
        kafkaProperties.getConsumer().setClientId(null);
        ConsumerFactory consumerFactory = new DefaultKafkaConsumerFactory(kafkaProperties.buildConsumerProperties());

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(ConcurrentKafkaListenerContainerFactory.class);
        beanDefinitionBuilder.addPropertyValue("consumerFactory", consumerFactory);
        beanDefinitionBuilder.addPropertyValue("concurrency", KafkaPropertiesReader.getListenerConcurrency(kafkaProperties));
        beanDefinitionBuilder.addPropertyValue("containerProperties.pollTimeout", KafkaPropertiesReader.getListenerPollTimeout(kafkaProperties));

        String beanName = kafkaProperties.getConsumer().getProperties().get(KafkaConst.LISTENER_NAMES_PROP);
        if (StringUtils.isEmpty(beanName)) {
            beanName = entry.getKey() + "_consumer_factory";
        }
        if (!defaultFactory && !applicationContext.containsBean(KafkaConst.DEFAULT_CONSUMER_FACTORY_BEAN_NAME)) {
            getBeanFactory().registerBeanDefinition(KafkaConst.DEFAULT_CONSUMER_FACTORY_BEAN_NAME, beanDefinitionBuilder.getBeanDefinition());
        }
        getBeanFactory().registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        // 拷贝参数
        KafkaProperties.Listener listener = kafkaProperties.getListener();
        ConcurrentKafkaListenerContainerFactory concurrentKafkaListenerContainerFactory = (ConcurrentKafkaListenerContainerFactory) applicationContext.getBean(beanName);
        ContainerProperties factoryContainerProperties = concurrentKafkaListenerContainerFactory.getContainerProperties();
        if (kafkaProperties.getConsumer() != null) {
            factoryContainerProperties.setGroupId(kafkaProperties.getConsumer().getGroupId());
        }
        if (null != listener.getAckMode()) {
            factoryContainerProperties.setAckMode(listener.getAckMode());
        }
        if (null != listener.getAckCount()) {
            factoryContainerProperties.setAckCount(listener.getAckCount());
        }
        if (null != listener.getAckTime()) {
            factoryContainerProperties.setAckTime(listener.getAckTime().toMillis());
        }
        if (KafkaPropertiesReader.isBatchListener(kafkaProperties)) {
            concurrentKafkaListenerContainerFactory.setBatchListener(true);
        }
        return beanDefinitionBuilder.getBeanDefinition();
    }

    private DefaultListableBeanFactory getBeanFactory() {
        return (DefaultListableBeanFactory) ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    }
}
