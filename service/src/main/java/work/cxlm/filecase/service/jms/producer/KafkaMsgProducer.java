package work.cxlm.filecase.service.jms.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import work.cxlm.filecase.kafka.config.CustomerKafkaTemplate;

import java.util.concurrent.TimeUnit;

/**
 * create 2021/4/7 10:24
 *
 * @author Chiru
 */
@Component
@Slf4j
public class KafkaMsgProducer {

    private CustomerKafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public void setKafkaTemplate(CustomerKafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 异步发消息
     *
     * @param topic   所属 topic
     * @param message 消息内容，需要转为字符串（序列化）
     */
    public void sendKafkaMsg(String topic, String message) {
        KafkaTemplate<String, String> template = kafkaTemplate.getTemplateByTopic(topic);
        try {
            long beginTime = System.currentTimeMillis();

            ListenableFuture<SendResult<String, String>> future = template.send(topic, message);
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onFailure(@NonNull Throwable ex) {
                    log.error("发送 Kafka 消息失败, 消息: {}, 异常: {}", message, ex.getMessage());
                }

                @Override
                public void onSuccess(@Nullable SendResult<String, String> result) {
                    if (result == null) {
                        log.info("发送 Kafka 消息成功，没有得到返回信息");
                    } else {
                        log.info("发送 Kafka 消息成功 partition: {} offset: {} message: {}",
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), message);
                    }
                }
            });

            log.info("发送 Kafka 消息: threadId: {}, time: {}, topic: {}, msg: {}",
                    Thread.currentThread().getId(), System.currentTimeMillis() - beginTime, topic, message);
        } catch (Exception e) {
            log.warn("kafka消息发送异常: {}", message, e);
        }
    }

    /**
     * 同步发消息
     *
     * @param topic   所属 topic
     * @param message 消息内容，需要转为字符串（序列化）
     */
    public void sendKafkaMsgSync(String topic, String message) {
        KafkaTemplate<String, String> msgTemplate = kafkaTemplate.getTemplateByTopic(topic);
        try {
            msgTemplate.send(topic, message).get(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("kafka 消息发送异常, topic: {}, msg:{}", topic, message, e);
        }
    }

}
