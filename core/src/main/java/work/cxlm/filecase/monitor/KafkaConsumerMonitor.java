package work.cxlm.filecase.monitor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * create 2021/4/8 11:12
 *
 * @author Chiru
 */
@Slf4j
public class KafkaConsumerMonitor implements ConsumerInterceptor<String, String> {
    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> consumerRecords) {
        for (ConsumerRecord<String, String> record : consumerRecords) {
            log.info("consumer monitor: Kafka 消费, topic: {}", record.topic());
        }
        return consumerRecords;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> map) {
        log.info("consumer monitor: Kafka 提交: {}", map.toString());
    }

    @Override
    public void close() {
        log.info("consumer monitor: Kafka 关闭");
    }

    @Override
    public void configure(Map<String, ?> map) {
        log.info("consumer monitor: Kafka 配置: {}", map.toString());
    }
}
