package http.request;

import http.Exceptions;
import http.callback.Callback;

import java.util.Map;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * create by 马杨茗 on 2016/07/04
 * 封装的一些OkHttp请求所需要的一些基本参数请求所需要的方式
 * @version 1.0
 */
public abstract class OkHttpRequest
{
    protected String url;
    protected Object tag;
    protected Map<String, String> params;
    protected Map<String, String> headers;
    protected int id;

    protected Request.Builder builder = new Request.Builder();

    protected OkHttpRequest(String url, Object tag,
                            Map<String, String> params, Map<String, String> headers,int id)
    {
        this.url = url;
        this.tag = tag;
        this.params = params;
        this.headers = headers;
        this.id = id ;

        if (url == null)
        {
            Exceptions.illegalArgument("url can not be null.");
        }

        initBuilder();
    }



    /**
     * 初始化一些基本参数 url , tag , headers
     * 此处可以通过cacheControl(CacheControl cacheControl)设置网络缓存
     * 有两个个参数可供设置
     * CacheControl.FORCE_CACHE 和CacheControl.FORCE_NETWORK分别表示只从缓存获取数据和只通过网络请求获取数据
     * 设置tag则可以通过tag进行取消此次请求
     */
    private void initBuilder()
    {
        builder.url(url).tag(tag);
        appendHeaders();
    }

    /**
     * 子类实现这个方法来重新构造RequestBody
     * @return
     */
    protected abstract RequestBody buildRequestBody();

    protected RequestBody wrapRequestBody(RequestBody requestBody, final Callback callback)
    {
        return requestBody;
    }
    /**
     * 子类实现这个方法通过RequestBody来重新构造Request
     * @return
     */
    protected abstract Request buildRequest(RequestBody requestBody);

    public RequestCall build()
    {
        return new RequestCall(this);
    }

    public Request generateRequest(Callback callback)
    {
        RequestBody requestBody = buildRequestBody();
        RequestBody wrappedRequestBody = wrapRequestBody(requestBody, callback);
        Request request = buildRequest(wrappedRequestBody);
        return request;
    }

    /**
     * 目前这种添加Head的方式有一个bug，就是同一个key无法对应多个Value，但是OkHttp是允许这种情况的
     */
    protected void appendHeaders()
    {
        Headers.Builder headerBuilder = new Headers.Builder();
        if (headers != null && !headers.isEmpty())
        {
            for (String key : headers.keySet())
            {
                headerBuilder.add(key, headers.get(key));
            }
        }
        builder.headers(headerBuilder.build());
    }
    public int getId()
    {
        return id  ;
    }
}
