package work.cxlm.filecase.redis.properties;

import lombok.Data;
import org.redisson.config.ReadMode;
import org.redisson.config.SubscriptionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REDIS 配置解析类
 * create 2021/4/11 15:09
 *
 * @author Chiru
 */
@ConfigurationProperties("redis")
@Data
public class RedisProperties {

    private Map<String, ClientProperties> clients;

    @Data
    public static class ClientProperties {
        /**
         * 客户端模式枚举
         */
        public enum Mode {
            /**
             * 单服务器模式
             */
            SINGLE,
            /**
             * 主从模式
             */
            MASTER_SLAVE,
            /**
             * 哨兵模式
             */
            SENTINEL,
            /**
             * 集群模式
             */
            CLUSTER,
            /**
             * 高可用集群模式
             */
            REPLICATED
        }

        /**
         * 客户端类型
         */
        public enum Type {
            NORMAL, REACTIVE, RX
        }

        /**
         * 客户端模式
         */
        private Mode mode = Mode.SINGLE;

        /**
         * 客户端类型
         */
        private Type type = Type.NORMAL;

        /**
         * 数据库编号
         */
        private Integer database = 0;

        /**
         * 实例密码
         */
        private String password;

        /**
         * 单实例模式的服务端地址，redis://ip:port
         */
        private String address;

        /**
         * 主从模式的主节点地址，redis://ip:port
         */
        private String masterAddress;

        /**
         * 主从模式的从节点地址，redis://ip:port 列表
         */
        private List<String> slaveAddress = new ArrayList<>();

        /**
         * 哨兵模式主服务器名称
         */
        private String masterName;

        /**
         * 哨兵模式的节点地址列表，redis://ip:port 列表
         */
        private List<String> sentinelAddress = new ArrayList<>();

        /**
         * 集群模式的节点列表，redis://ip:port 列表
         */
        private List<String> clusterAddress = new ArrayList<>();

        /**
         * 多实例的读取模式
         */
        private ReadMode readMode = ReadMode.SLAVE;

        /**
         * 多实例订阅模式
         */
        private SubscriptionMode subscriptionMode = SubscriptionMode.SLAVE;

        /**
         * 连接池最小连接数
         */
        private Integer minIdle = 30;

        /**
         * 连接池最大容量
         */
        private Integer maxActive = 100;

        /**
         * 连接池中连接的最大空闲时间，超过此时间的连接将被回收，单位毫秒
         */
        private Integer maxIdleTimeout = 300_000;

        /**
         * 主节点连接池最小连接数
         */
        private Integer masterMinIdle = 30;

        /**
         * 主节点连接池最大容量
         */
        private Integer masterMaxActive = 100;

        /**
         * 从节点连接池最小连接数
         */
        private Integer slaveMinIdle = 30;

        /**
         * 从节点连接池最大容量
         */
        private Integer slaveMaxActive = 100;

        /**
         * 订阅连接池最小连接数
         */
        private Integer subscriptionMinIdle = 1;

        /**
         * 订阅连接池最大容量
         */
        private Integer subscriptionMaxActive = 50;

        /**
         * 连接建立的超时时间，单位毫秒
         */
        private Integer connectTimeout = 3_000;

        /**
         * 命令执行超时时间，单位毫秒
         */
        private Integer timeout = 300;

        /**
         * 命令失败重试次数
         */
        private Integer retryAttempts = 1;

        /**
         * 命令失败后重试的时间间隔，单位毫秒
         */
        private Integer retryInterval = 100;

        /**
         * 连接池编码器的 Bean 名称，默认为 org.redisson.codec.JsonJacksonCodec
         */
        private String codecBeanName;
    }
}
