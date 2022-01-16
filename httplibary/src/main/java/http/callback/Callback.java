package http.callback;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 用于将服务器的数据返回给主线程
 * create by 马杨茗 on 2016/07/04
 * @param <T>
 * @version 1.0
 */
public abstract class Callback<T>
{
    public boolean requestSuccess = false;//网络数据返回结果是否正确是否请求成功
    /**
     * UI Thread
     * 网络请求前
     * @param request
     */
    public void onBefore(Request request, int id)
    {
    }

    /**
     * UI Thread
     * 网络请求后
     * @param
     */
    public void onAfter(int id)
    {
    }

    /**
     * UI Thread
     * 进度条
     * @param progress 0.0表示没有开始 1.0表示下载完成
     * @param total 总进度
     */
    public void inProgress(float progress, long total , int id)
    {

    }

    /**
     * 如果你在parsenetworkresponse()解析响应代码，你应该把这个方法返回true。
     * 该方法主要是判断返回的状态码是否是成功的状态码，200~300范围内都算成功
     * @param response
     * @return
     */
    public boolean validateReponse(Response response, int id)
    {
        return response.isSuccessful();
    }

    /**
     * Thread Pool Thread
     * 解析Jason
     * @param response
     */
    public abstract T parseNetworkResponse(Response response, int id) throws Exception;

    public abstract void onError(Call call, Exception e, int id);

    public abstract void onResponse(T response, int id);

    /**
     * 倘若想对返回的数据进行解析，可以在parseNetworkResponse方法中进行
     * 通过parseNetworkResponse方法不仅可以获取返回的String数据类型，也可以获取byte[]和InputStream数据类型
     */
    public static Callback CALLBACK_DEFAULT = new Callback()
    {

        @Override
        public Object parseNetworkResponse(Response response, int id) throws Exception
        {
            return null;
        }

        @Override
        public void onError(Call call, Exception e, int id)
        {

        }

        @Override
        public void onResponse(Object response, int id)
        {

        }
    };

}