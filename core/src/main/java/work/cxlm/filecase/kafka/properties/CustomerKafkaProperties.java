package work.cxlm.filecase.kafka.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 * Kafka 配置
 * create 2021/4/7 14:22
 *
 * @author Chiru
 */
@Data
@ConfigurationProperties("kafka")
public class CustomerKafkaProperties {

    /**
     * 配置信息
     */
    private Map<String, KafkaProperties> config;
}
