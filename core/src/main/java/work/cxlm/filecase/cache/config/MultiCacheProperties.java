package work.cxlm.filecase.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;

/**
 * create 2021/4/18 20:55
 *
 * @author Chiru
 */
@ConfigurationProperties("cache")
@Data
public class MultiCacheProperties {

    /**
     * 各层缓存名
     */
    private ArrayList<String> layers;
}
