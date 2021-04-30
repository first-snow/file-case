package work.cxlm.filecase.response;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * create 2021/4/22 11:57
 *
 * @author Chiru
 */
@Getter
@ToString
public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private int code;

    /** 状态描述 */
    private final String msg;

    /** 实际返回的数据 */
    private T data;

    private Message() {
        this(HttpStatus.OK);
    }

    private Message(HttpStatus status) {
        code = status.value();
        msg = status.getReasonPhrase();
    }

    private Message(HttpStatus status, T data) {
        this(status, null, data);
    }

    private Message(HttpStatus status, String msg, T data) {
        if (msg != null && !msg.isEmpty()) {
            this.msg = msg;
        } else {
            this.msg = status.getReasonPhrase();
        }
        if (data != null) {
            this.data = data;
        }
    }

    public static <T> Message<T> ok() {
        return new Message<>();
    }

    public static <T> Message<T> ok(T data) {
        return new Message<>(HttpStatus.OK, data);
    }

    public static <T> Message<T> ok(String msg, T data) {
        return new Message<>(HttpStatus.OK, msg, data);
    }

    public static <T> Message<T> error() {
        return new Message<>(HttpStatus.BAD_REQUEST);
    }

    public static <T> Message<T> error(String msg) {
        return new Message<>(HttpStatus.BAD_REQUEST, msg, null);
    }

    public static <T> Message<T> error(T data) {
        return new Message<>(HttpStatus.BAD_REQUEST, null, data);
    }

    public static <T> Message<T> error(String msg, T data) {
        return new Message<>(HttpStatus.BAD_REQUEST, msg, data);
    }

    public static <T> Message<T> build(HttpStatus status, String msg) {
        return new Message<>(status, msg, null);
    }

    public static <T> Message<T> build(HttpStatus status, T data) {
        return new Message<>(status, null, data);
    }

    public static <T> Message<T> build(HttpStatus status, String msg, T data) {
        return new Message<>(status, msg, data);
    }

}
