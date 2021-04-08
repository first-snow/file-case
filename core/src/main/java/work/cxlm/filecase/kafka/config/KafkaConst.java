package work.cxlm.filecase.kafka.config;

/**
 * create 2021/4/7 15:35
 *
 * @author Chiru
 */
public interface KafkaConst {

    String LISTENER_NAMES_PROP = "listener-names";

    String ENABLE_AUTO_GENERATE_GROUP_ID_PROP = "enable-auto-generate-group-id";

    String DEFAULT_CONSUMER_FACTORY_BEAN_NAME = "kafkaListenerContainerFactory";

    String TOPIC_DELIMITER = ",";
}
