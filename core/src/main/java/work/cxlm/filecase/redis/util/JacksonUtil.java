package work.cxlm.filecase.redis.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import work.cxlm.filecase.exception.JacksonConvertException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * create 2021/4/16 18:46
 *
 * @author Chiru
 */
@UtilityClass
@Slf4j
public class JacksonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();

        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        OBJECT_MAPPER.getSerializerProvider().setNullValueSerializer(new JsonSerializer<Object>() {
            @Override
            public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                    throws IOException {
                jsonGenerator.writeString("");
            }
        });
    }

    public ObjectMapper getObjectMapper() {
        return JacksonUtil.OBJECT_MAPPER;
    }

    /**
     * 将指定对象转化为 JSON 字符串
     *
     * @param obj 要转化的对象
     * @return JSON 字符串
     */
    public String objectToString(Object obj) {
        if (null == obj) {
            return null;
        }
        if (obj instanceof String) {
            return obj.toString();
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Jackson 转化错误，源对象：{}", obj, e);
        }
        return "";
    }

    /**
     * 将 Json 转化为指定对象
     *
     * @param content json 字符串
     * @param clazz   目标类对象
     * @param <T>     目标类的类型参数
     * @return 转化后的对象
     */
    public <T> T jsonToObject(String content, Class<T> clazz) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(content, clazz);
        } catch (IOException e) {
            throw new JacksonConvertException("JSON 转化为对象出错，源 JSON：" + content, e);
        }
    }

    /**
     * json 字符串转对象 list
     *
     * @param rawJson  源 JSON 字符串
     * @param beanType 目标类型
     * @param <T>      目标类的类型参数
     * @return 目标类的实例列表
     */
    public <T> List<T> jsonToObjectList(String rawJson, Class<T> beanType) {
        if (StringUtils.isEmpty(rawJson)) {
            return Lists.newArrayList();
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(List.class, beanType);
        try {
            return OBJECT_MAPPER.readValue(rawJson, javaType);
        } catch (IOException e) {
            throw new JacksonConvertException("json 转化为 List 出错，源 JSON：" + rawJson, e);
        }
    }

    /**
     * json 字符串转 Map
     *
     * @param jsonString 源 JSON 字符串
     * @param clazz      目标类型
     * @param <T>        目标类的类型参数
     * @return 指定类型为值的 Map
     */
    public <T> Map<String, T> jsonToMap(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        Map<String, T> result = new HashMap<>(16);
        try {
            Map<String, Map<String, Object>> map = OBJECT_MAPPER.readValue(jsonString, new TypeReference<Map<String, T>>() {
            });
            for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                result.put(entry.getKey(), OBJECT_MAPPER.convertValue(entry.getValue(), clazz));
            }
        }catch (IOException e){
            throw new JacksonConvertException("Json 转化为 Map 出错，源 JSON："+jsonString, e);
        }

        return result;
    }

}