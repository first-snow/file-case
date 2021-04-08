package work.cxlm.filecase.exception;

/**
 * 集群、服务器未找到时抛出
 * create 2021/4/7 21:23
 *
 * @author Chiru
 */
public class ServerNotFoundException extends RuntimeException {

    public ServerNotFoundException(String msg) {
        super(msg);
    }

    public ServerNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
