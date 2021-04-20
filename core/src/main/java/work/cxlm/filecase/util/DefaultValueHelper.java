package work.cxlm.filecase.util;

import lombok.experimental.UtilityClass;

/**
 * create 2021/4/16 12:28
 *
 * @author Chiru
 */
@UtilityClass
@SuppressWarnings("all")
public class DefaultValueHelper {
    // 各种基本类型的默认值
    private static boolean booleanDefault;
    private static byte byteDefault;
    private static short shortDefault;
    private static int intDefault;
    private static long longDefault;
    private static char charDefault;
    private static float floatDefault;
    private static double doubleDefault;

    public Object getClassDefaultValue(Class<?> clazz) {
        String className = clazz.getName();
        switch (className) {
            case "boolean":
                return booleanDefault;
            case "byte":
                return byteDefault;
            case "short":
                return shortDefault;
            case "int":
                return intDefault;
            case "long":
                return longDefault;
            case "char":
                return charDefault;
            case "float":
                return floatDefault;
            case "double":
                return doubleDefault;
            default:
                return null;
        }
    }
}
