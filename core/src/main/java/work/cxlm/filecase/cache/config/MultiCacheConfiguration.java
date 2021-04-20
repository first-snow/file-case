package work.cxlm.filecase.cache.config;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import work.cxlm.filecase.cache.AbstractStringCacheLayer;
import work.cxlm.filecase.cache.CacheLayerBuilder;
import work.cxlm.filecase.cache.MultiStringCache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * create 2021/4/19 21:11
 *
 * @author Chiru
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MultiCacheProperties.class)
@Import(MultiCacheConfiguration.CacheLayerRegister.class)
public class MultiCacheConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 注册 MultiCache 的 Bean
     */
    @Bean
    public MultiStringCache generateMultiCacheBean(MultiCacheProperties properties) {
        List<AbstractStringCacheLayer> layers = properties.getLayers().stream()
                .map(layerName -> (AbstractStringCacheLayer) applicationContext.getBean(layerName + "CacheLayer"))
                .collect(Collectors.toList());
        return new MultiStringCache(layers);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    static class CacheLayerRegister implements BeanFactoryPostProcessor, Ordered, EnvironmentAware {

        private final AtomicBoolean registered = new AtomicBoolean(false);
        private List<String> layers;

        @Override
        public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
            if (beanFactory instanceof BeanDefinitionRegistry && !registered.get()) {
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                // 读取各项配置
                Set<String> layerNameSet = new HashSet<>(layers);
                layerNameSet.forEach(layer -> {
                    val layerName = layer + "CacheLayer";
                    val builder = BeanDefinitionBuilder
                            .genericBeanDefinition(CacheLayerBuilder.class)
                            .addConstructorArgValue(layer)
                            .setFactoryMethod("buildStringCacheLayer");

                    AbstractBeanDefinition bd = builder.getBeanDefinition();

                    registry.registerBeanDefinition(layerName, bd);
                    log.info("注册缓存层: {}", layerName);
                });
            }
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            layers = Binder.get(environment)
                    .bind("cache.layers", Bindable.listOf(String.class))
                    .orElse(Lists.emptyList());
        }
    }
}
