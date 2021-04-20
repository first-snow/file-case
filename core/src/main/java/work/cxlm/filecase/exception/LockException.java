package work.cxlm.filecase.exception;

/**
 * create 2021/4/13 16:54
 *
 * @author Chiru
 */
public class LockException extends RuntimeException {

    public LockException(String msg) {
        super(msg);
    }

    public LockException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
