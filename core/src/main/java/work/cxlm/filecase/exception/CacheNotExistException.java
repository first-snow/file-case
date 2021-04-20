package work.cxlm.filecase.exception;

/**
 * create 2021/4/16 18:08
 *
 * @author Chiru
 */
public class CacheNotExistException extends RuntimeException {

    public CacheNotExistException(String msg) {
        super(msg);
    }

    public CacheNotExistException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
