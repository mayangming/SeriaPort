package http;

/**
 * http异常管理类
 * create by 马杨茗 on 2016/07/05
 * @author 马杨茗 date 2016/7/4
 * @version 1.0
 */
public class Exceptions
{
    public static void illegalArgument(String msg, Object... params)
    {
        throw new IllegalArgumentException(String.format(msg, params));
    }


}
