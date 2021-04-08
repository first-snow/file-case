package work.cxlm.filecase.monitor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

/**
 * create 2021/4/8 11:26
 *
 * @author Chiru
 */
@Slf4j
public class KafkaProducerMonitor implements ProducerInterceptor<String, String> {

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> producerRecord) {
        log.info("producer monitor: Kafka 发送消息, topic {}", producerRecord.topic());
        return producerRecord;
    }

    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
        if (e == null) {
            log.info("producer monitor: 成功处理: topic: {}", recordMetadata.topic());
        } else {
            log.error("producer monitor: 处理失败: topic: {}, msg: {}", recordMetadata.topic(), e.getMessage());
        }
    }

    @Override
    public void close() {
        log.info("producer monitor: Kafka 关闭");
    }

    @Override
    public void configure(Map<String, ?> map) {
        log.info("producer monitor: Kafka 配置: {}", map.toString());
    }
}
