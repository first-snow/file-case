package work.cxlm.filecase.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;

/**
 * create 2021/4/7 18:43
 *
 * @author Chiru
 */
@Slf4j
public class KafkaProducerListener<K, V> implements ProducerListener<K, V> {

    public KafkaProducerListener() {
    }

    @Override
    public void onSuccess(String topic, Integer partition, K key, V value, RecordMetadata recordMetadata) {
        log.info("生产 Kafka 消息成功, topic: {}, partition: {}, key: {}, value: {}, offset: {}",
                topic, partition, key, value, recordMetadata.offset());
    }

    @Override
    public void onError(String topic, Integer partition, K key, V value, Exception exception) {
        log.info("生产 Kafka 消息出错, topic: {}, partition:{}, key:{}, value:{}", topic, partition, key, value, exception);
    }
}
