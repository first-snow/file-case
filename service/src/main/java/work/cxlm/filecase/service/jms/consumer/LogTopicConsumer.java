package work.cxlm.filecase.service.jms.consumer;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * create 2021/4/7 10:25
 *
 * @author Chiru
 */
@Component
@Slf4j
public class LogTopicConsumer {

    @KafkaListener(topics = {"${kafka.config.log.consumer.properties.topics[0]}"},
            containerFactory = "${kafka.config.log.consumer.properties.listener-names}")
    public void onMessage(List<String> data) {
        for (val msg : data) {
            // TODO: 拓展日志处理内容
            log.info("消费消息：{}", msg);
        }
    }
}
