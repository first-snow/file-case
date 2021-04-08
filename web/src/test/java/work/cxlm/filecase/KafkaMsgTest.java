package work.cxlm.filecase;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import work.cxlm.filecase.service.jms.consumer.LogTopicConsumer;
import work.cxlm.filecase.service.jms.producer.KafkaMsgProducer;

/**
 * create 2021/4/8 12:23
 *
 * @author Chiru
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileCaseApplication.class)
public class KafkaMsgTest {

    private LogTopicConsumer consumer;
    private KafkaMsgProducer producer;

    @Value("${kafka.config.log.producer.properties.topics}")
    private String producerTopic;

    @Value("kafka.config.log.consumer.properties.topics[0]")
    private String consumerTopic;

    @Autowired
    public void setConsumer(LogTopicConsumer consumer) {
        this.consumer = consumer;
    }

    @Autowired
    public void setProducer(KafkaMsgProducer producer) {
        this.producer = producer;
    }

    @Test
    public void produceMsgTest() {
        producer.sendKafkaMsg(producerTopic, "{text: \"Test message body.\"}");
        producer.sendKafkaMsgSync(producerTopic, "{text: \"Test message body.\"}");
        int waitTime = 300_0000;
        // 等待消息消费
        while (waitTime-- > 0) {
            Thread.yield();
            // Thread.onSpinWait(); Java8 不支持
        }
    }

    @Test
    public void consumeMsgTest() {
        consumer.onMessage(Lists.list("Test (Mock) message body."));
    }
}
