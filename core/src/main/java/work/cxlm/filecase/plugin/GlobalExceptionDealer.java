package work.cxlm.filecase.plugin;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;
import work.cxlm.filecase.exception.LockException;
import work.cxlm.filecase.redis.util.JacksonUtil;
import work.cxlm.filecase.response.Message;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常兜底处理器
 * create 2021/4/22 11:43
 *
 * @author Chiru
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionDealer {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public Message<?> handleRuntimeException(HttpServletRequest request, Exception e) {
        logStackTrace(request, e);
        if (e instanceof BindException) {
            BindException exception = (BindException) e;
            RequestContext rc = new RequestContext(request);
            FieldError error = exception.getBindingResult().getFieldErrors().get(0);
            String errorTips = String.format("字段绑定错误：字段：%s, Msg：%s", error.getField(), rc.getMessage(error));
            return Message.error(errorTips);
        }
        return Message.build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Message<?> handleRemainException(HttpServletRequest request, Throwable e) {
        logStackTrace(request, e);
        return Message.build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(LockException.class)
    @ResponseStatus(HttpStatus.OK)
    public Message<?> handleLockException(HttpServletRequest request, Throwable e) {
        logStackTrace(request, e);
        return Message.build(HttpStatus.OK, e.getMessage());
    }

    /**
     * 打印堆栈和入参
     */
    private void logStackTrace(HttpServletRequest request, Throwable e) {
        boolean isRestful = false;
        Object pathVariables = request.getAttribute(View.PATH_VARIABLES);
        if (pathVariables instanceof Map) {
            isRestful = true;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> params = isRestful
                ? new HashMap<>((Map<String, Object>) pathVariables)
                : Maps.transformValues(request.getParameterMap(), arr -> arr.length == 0 ? null : arr.length == 1 ? arr[0] : arr);
        Map<String, Object> logMap = new HashMap<>(10);
        logMap.put("path", request.getRequestURI());
        logMap.put("from", request.getRemoteAddr());
        logMap.put("handle", e.getClass().getName());
        logMap.put("message", e.getMessage());
        logMap.put("params", params);
        log.error(JacksonUtil.objectToString(logMap), e);
    }

}
