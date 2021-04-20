package work.cxlm.filecase.exception;

/**
 * create 2021/4/16 19:19
 *
 * @author Chiru
 */
public class JacksonConvertException extends RuntimeException{

    public JacksonConvertException(String msg) {
        super(msg);
    }

    public JacksonConvertException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
