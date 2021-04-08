package work.cxlm.filecase.kafka.utils;

import io.netty.util.internal.StringUtil;
import lombok.experimental.UtilityClass;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import work.cxlm.filecase.kafka.config.KafkaConst;

import java.util.Map;
import java.util.Optional;

/**
 * 复制解析 Kafka 配置参数的工具类
 * create 2021/4/7 15:05
 *
 * @author Chiru
 */
@UtilityClass
public class KafkaPropertiesReader {

    /**
     * 是否配置了 bootstrap.servers 参数（Kafka 集群）
     *
     * @param kafkaProperties KafkaProperties 实例
     */
    public boolean bootStrapServersNotPresent(KafkaProperties kafkaProperties) {
        if (kafkaProperties == null) {
            return true;
        }
        Map<String, Object> propertiesMap = kafkaProperties.buildProducerProperties();
        return propertiesMap == null || propertiesMap.get("bootstrap.servers") == null;
    }


    /**
     * 获取并发度配置
     *
     * @param kafkaProperties KafkaProperties 实例
     */
    public static int getListenerConcurrency(KafkaProperties kafkaProperties) {

        if (null == kafkaProperties || null == kafkaProperties.getListener() ||
                null == kafkaProperties.getListener().getConcurrency()) {
            return 3;
        }
        return kafkaProperties.getListener().getConcurrency();
    }

    /**
     * 获取轮询时间配置
     *
     * @param kafkaProperties KafkaProperties 实例
     */
    public static long getListenerPollTimeout(KafkaProperties kafkaProperties) {
        if (null == kafkaProperties || null == kafkaProperties.getListener() ||
                null == kafkaProperties.getListener().getPollTimeout()) {
            return 3000L;
        }
        return kafkaProperties.getListener().getPollTimeout().toMillis();
    }

    public String getGroupId(KafkaProperties kafkaProperties, String listenerName, String applicationName) {
        if (isAutoGenerateGroupId(kafkaProperties)) {
            String groupId = kafkaProperties.getConsumer().getGroupId();
            if (null == groupId) {
                groupId = StringUtil.EMPTY_STRING;
            }
            return String.format("%s-%s-%s", listenerName, applicationName, groupId);
        } else {
            return kafkaProperties.getConsumer().getGroupId();
        }
    }

    /**
     * 是否自动生成 GroupId，即 enable-auto-generate-group-id 参数是否配置为 true
     *
     * @param kafkaProperties KafkaProperties 实例
     */
    private boolean isAutoGenerateGroupId(KafkaProperties kafkaProperties) {
        return Optional.ofNullable(kafkaProperties.getConsumer())
                .map(consumer -> "true".equalsIgnoreCase(consumer.getProperties().get(KafkaConst.ENABLE_AUTO_GENERATE_GROUP_ID_PROP)))
                .orElse(false);
    }

    /**
     * 判断是否批量消费。
     *
     * @param kafkaProperties KafkaProperties 实例
     */
    public static boolean isBatchListener(KafkaProperties kafkaProperties) {
        return Optional.ofNullable(kafkaProperties.getConsumer())
                .map(consumer -> "true".equalsIgnoreCase(consumer.getProperties().get("is-batch-listener")))
                .orElse(false);
    }
}
