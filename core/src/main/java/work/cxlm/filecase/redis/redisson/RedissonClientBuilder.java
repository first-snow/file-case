package work.cxlm.filecase.redis.redisson;

import lombok.val;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RedissonRxClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.*;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.redis.properties.RedisProperties;

/**
 * Redisson 客户端构造器
 * create 2021/4/11 15:27
 *
 * @author Chiru
 */
public class RedissonClientBuilder {

    /**
     * 客户端名称
     */
    private final String name;

    /**
     * 配置属性对象
     */
    private final RedisProperties.ClientProperties properties;

    /**
     * ApplicationContext上下文
     */
    private final ApplicationContext applicationContext;

    /**
     * 从客户端配置属性构造对象
     *
     * @param properties 配置属性对象
     */
    public RedissonClientBuilder(@NonNull String name, @NonNull RedisProperties.ClientProperties properties,
                                 @NonNull ApplicationContext context) {
        Assert.notNull(name, "客户端名称不能为 null");
        Assert.notNull(properties, "ClientProperties 不能为 null");
        Assert.notNull(context, "ApplicationContext 不能为 null");

        this.name = name;
        this.properties = properties;
        this.applicationContext = context;
    }

    /**
     * 生成配置
     *
     * @return 配置对象
     */
    protected Config buildConfig() {
        val config = new Config();
        if (null != this.properties.getCodecBeanName()) {
            config.setCodec(applicationContext.getBean(this.properties.getCodecBeanName(), Codec.class));
        }
        if (RedisProperties.ClientProperties.Mode.SINGLE.equals(this.properties.getMode())) {
            // 单点
            buildServer(config.useSingleServer());
        } else if (RedisProperties.ClientProperties.Mode.MASTER_SLAVE.equals(this.properties.getMode())) {
            // 主从模式
            buildServer(config.useMasterSlaveServers());
        } else if (RedisProperties.ClientProperties.Mode.SENTINEL.equals(this.properties.getMode())) {
            // Sentinel模式
            buildServer(config.useSentinelServers());
        } else if (RedisProperties.ClientProperties.Mode.CLUSTER.equals(this.properties.getMode())) {
            // 集群模式
            buildServer(config.useClusterServers());
        } else if (RedisProperties.ClientProperties.Mode.REPLICATED.equals(this.properties.getMode())) {
            // 托管集群模式
            buildServer(config.useReplicatedServers());
        }
        return config;
    }

    private void buildMasterSlaveServerConfig(BaseMasterSlaveServersConfig<?> config) {
        config.setClientName(name)
                .setPassword(StringUtils.isEmpty(properties.getPassword()) ? null : properties.getPassword())
                .setReadMode(properties.getReadMode())
                .setSubscriptionMode(properties.getSubscriptionMode())
                .setMasterConnectionMinimumIdleSize(properties.getMasterMinIdle())
                .setMasterConnectionPoolSize(properties.getMasterMaxActive())
                .setSlaveConnectionMinimumIdleSize(properties.getSlaveMinIdle())
                .setSlaveConnectionPoolSize(properties.getSlaveMaxActive())
                .setSubscriptionConnectionMinimumIdleSize(properties.getSubscriptionMinIdle())
                .setSubscriptionConnectionPoolSize(properties.getSubscriptionMaxActive())
                .setIdleConnectionTimeout(properties.getMaxIdleTimeout())
                .setDnsMonitoringInterval(2000)
                .setConnectTimeout(properties.getConnectTimeout())
                .setTimeout(properties.getTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
        ;
    }

    private void buildServer(ReplicatedServersConfig config) {
        buildMasterSlaveServerConfig(config);
        config.addNodeAddress(properties.getClusterAddress().toArray(new String[0]));
    }

    private void buildServer(ClusterServersConfig config) {
        buildMasterSlaveServerConfig(config);
        config.addNodeAddress(properties.getClusterAddress().toArray(new String[0]));
    }

    private void buildServer(MasterSlaveServersConfig config) {
        buildMasterSlaveServerConfig(config);
        config.setMasterAddress(properties.getMasterAddress())
                .addSlaveAddress(properties.getSlaveAddress().toArray(new String[0]))
                .setDatabase(properties.getDatabase());
    }

    private void buildServer(SentinelServersConfig config) {
        buildMasterSlaveServerConfig(config);
        config.setMasterName(properties.getMasterName())
                .addSentinelAddress(properties.getSentinelAddress().toArray(new String[0]))
                .setDatabase(properties.getDatabase());
    }

    private void buildServer(SingleServerConfig config) {
        // 单实例
        config.setClientName(name)
                .setAddress(properties.getAddress())
                .setPassword(StringUtils.isEmpty(properties.getPassword()) ? null : properties.getPassword())
                .setDatabase(properties.getDatabase())
                .setConnectionMinimumIdleSize(properties.getMinIdle())
                .setConnectionPoolSize(properties.getMaxActive())
                .setSubscriptionConnectionMinimumIdleSize(properties.getSubscriptionMinIdle())
                .setSubscriptionConnectionPoolSize(properties.getSubscriptionMaxActive())
                .setIdleConnectionTimeout(properties.getMaxIdleTimeout())
                .setDnsMonitoringInterval(2000)
                .setConnectTimeout(properties.getConnectTimeout())
                .setTimeout(properties.getTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval());
    }

    /**
     * 构建 RedissonClient
     *
     * @return RedissonClient
     */
    public RedissonClient build() {
        return Redisson.create(buildConfig());
    }

    /**
     * 构建 Reactive 的 Client
     *
     * @return RedissonReactiveClient
     */
    public RedissonReactiveClient buildReactiveClient() {
        return Redisson.createReactive(buildConfig());
    }

    /**
     * 构建 Rx 的 Client
     *
     * @return RedissonRxClient
     */
    public RedissonRxClient buildRxClient() {
        return Redisson.createRx(buildConfig());
    }
}
