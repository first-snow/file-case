package work.cxlm.filecase.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import work.cxlm.filecase.exception.ServerNotFoundException;
import work.cxlm.filecase.kafka.properties.CustomerKafkaProperties;
import work.cxlm.filecase.kafka.utils.KafkaPropertiesReader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * create 2021/4/7 17:21
 *
 * @author Chiru
 */
@Slf4j
public class CustomerKafkaTemplate<K, V> implements KafkaOperations<K, V> {

    /**
     * topic - cluster 映射
     */
    private final Map<String, String> topicGroupMapping = new ConcurrentHashMap<>(64);

    /**
     * cluster - 模板 映射
     */
    private final Map<String, KafkaTemplate<K, V>> groupTemplateMapping = new ConcurrentHashMap<>(16);

    /**
     * 默认模板
     */
    private KafkaTemplate<K, V> defaultTemplate;

    public CustomerKafkaTemplate(CustomerKafkaProperties kafkaProperties) {
        buildTemplateMappings(kafkaProperties);
    }

    @SuppressWarnings("rawtypes, unchecked")
    private void buildTemplateMappings(CustomerKafkaProperties kafkaProperties) {
        if (null == kafkaProperties || null == kafkaProperties.getConfig()) {
            return;
        }

        for (Map.Entry<String, KafkaProperties> kafkaPropertiesEntry : kafkaProperties.getConfig().entrySet()) {
            if (KafkaPropertiesReader.bootStrapServersNotPresent(kafkaPropertiesEntry.getValue())) {
                continue;
            }

            ProducerFactory<K, V> producerFactory = new DefaultKafkaProducerFactory(kafkaPropertiesEntry.getValue().buildProducerProperties());
            KafkaTemplate<K, V> kafkaTemplate = new KafkaTemplate(producerFactory);
            kafkaTemplate.setProducerListener(new KafkaProducerListener());
            kafkaTemplate.setDefaultTopic(kafkaPropertiesEntry.getValue().getTemplate().getDefaultTopic());

            this.groupTemplateMapping.put(kafkaPropertiesEntry.getKey(), kafkaTemplate);
            if (this.defaultTemplate == null) {
                this.defaultTemplate = kafkaTemplate;
            }
            buildTopicGroupMappings(kafkaPropertiesEntry.getKey(), kafkaPropertiesEntry.getValue());
        }
    }

    private void buildTopicGroupMappings(String groupName, KafkaProperties kafkaProperties) {
        if (StringUtils.isEmpty(groupName) || kafkaProperties == null || kafkaProperties.getProducer() == null) {
            return;
        }

        if (StringUtils.hasText(kafkaProperties.getTemplate().getDefaultTopic())) {
            String topics = kafkaProperties.getTemplate().getDefaultTopic();
            for (String topic : topics.split(KafkaConst.TOPIC_DELIMITER)) {
                this.topicGroupMapping.put(topic, groupName);
            }
        }

        String topicsStr = "topics";
        if (StringUtils.hasText(kafkaProperties.getProducer().getProperties().get(topicsStr))) {
            String topics = kafkaProperties.getProducer().getProperties().get(topicsStr);
            for (String topic : topics.split(KafkaConst.TOPIC_DELIMITER)) {
                this.topicGroupMapping.put(topic, groupName);
            }
        }
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> sendDefault(@NonNull V data) {
        return defaultTemplate.sendDefault(data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> sendDefault(@NonNull K key, @NonNull V data) {
        return defaultTemplate.sendDefault(key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> sendDefault(@NonNull Integer partition, @NonNull K key, @NonNull V data) {
        return defaultTemplate.sendDefault(partition, key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> sendDefault(@NonNull Integer partition, @NonNull Long timestamp,
                                                          @NonNull K key, @NonNull V data) {
        return defaultTemplate.sendDefault(partition, timestamp, key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull String topic, @NonNull V data) {
        KafkaTemplate<K, V> templateByTopic = getTemplateByTopic(topic);
        return templateByTopic.send(topic, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull String topic, @NonNull K key, @NonNull V data) {
        KafkaTemplate<K, V> templateByTopic = getTemplateByTopic(topic);
        return templateByTopic.send(topic, key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull String topic, @NonNull Integer partition,
                                                   @NonNull K key, @NonNull V data) {
        KafkaTemplate<K, V> templateByTopic = getTemplateByTopic(topic);
        return templateByTopic.send(topic, partition, key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull String topic, @NonNull Integer partition,
                                                   @NonNull Long timestamp, @NonNull K key, @NonNull V data) {
        KafkaTemplate<K, V> templateByTopic = getTemplateByTopic(topic);
        return templateByTopic.send(topic, partition, timestamp, key, data);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull ProducerRecord<K, V> record) {
        KafkaTemplate<K, V> kafkaTemplate = getTemplateByTopic(record.topic());
        return kafkaTemplate.send(record);
    }

    @Override
    @NonNull
    public ListenableFuture<SendResult<K, V>> send(@NonNull Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        String topic = headers.get("kafka_topic", String.class);
        KafkaTemplate<K, V> kafkaTemplate = this.getTemplateByTopic(topic);
        return kafkaTemplate.send(message);
    }

    @Override
    @NonNull
    public List<PartitionInfo> partitionsFor(@NonNull String topic) {
        KafkaTemplate<K, V> kafkaTemplate = this.getTemplateByTopic(topic);
        return kafkaTemplate.partitionsFor(topic);
    }

    @Override
    @NonNull
    public Map<MetricName, ? extends Metric> metrics() {
        return defaultTemplate.metrics();
    }

    /**
     * 不推荐使用，可能使用 Transaction 会更好
     */
    @Override
    @NonNull
    public <T> T execute(@NonNull ProducerCallback<K, V, T> callback) {
        return defaultTemplate.execute(callback);
    }

    @Override
    @NonNull
    public <T> T executeInTransaction(@NonNull OperationsCallback<K, V, T> callback) {
        return defaultTemplate.executeInTransaction(callback);
    }

    /**
     * 不推荐使用
     */
    @Override
    public void flush() {
        defaultTemplate.flush();
    }

    @Override
    public void sendOffsetsToTransaction(@NonNull Map<TopicPartition, OffsetAndMetadata> offsets) {
        this.defaultTemplate.sendOffsetsToTransaction(offsets);

    }

    @Override
    public void sendOffsetsToTransaction(@NonNull Map<TopicPartition, OffsetAndMetadata> offsets, @NonNull String consumerGroupId) {
        this.defaultTemplate.sendOffsetsToTransaction(offsets, consumerGroupId);
    }

    /**
     * 通过 Topic 获取 template
     * 一言不合就抛异常，避免配置错误
     */
    @NonNull
    public KafkaTemplate<K, V> getTemplateByTopic(String topic) {
        String groupName = this.topicGroupMapping.get(topic);
        if (StringUtils.isEmpty(groupName)) {
            log.error("topic 未匹配到任何可用 servers, topic: {}", topic);
            throw new ServerNotFoundException("根据 topic: " + topic + " 找不到所在组，无法获得 server");
        } else {
            KafkaTemplate<K, V> kafkaTemplate = getTemplateByGroup(groupName);
            if (kafkaTemplate == null) {
                log.error("group 匹配不到 server, groupName: {}", groupName);
                throw new ServerNotFoundException("根据 group: " + groupName + " 找不到 server");
            } else {
                log.info("根据 group-name: {}, topic: {} 获取了 Template", groupName, topic);
                return kafkaTemplate;
            }
        }
    }

    /**
     * 通过 group 获取 template
     */
    public KafkaTemplate<K, V> getTemplateByGroup(String groupName) {
        return this.groupTemplateMapping.get(groupName);
    }
}
