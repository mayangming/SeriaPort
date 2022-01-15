package http.log;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 一个简单的拦截器，仅供打印日志
 * 在此处对Request的任何设置都会对请求产生影响
 * 具体使用方式参考以下链接
 * http://www.cnblogs.com/yuanchongjie/p/4962310.html
 * 关于打印响应值内容的方式参见：https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor
 * @author 马杨茗 date 2017/3/13
 * @version 1.0
 */
public class SimpleInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        // 注意:Chain.proceed(Request request)使请求能够继续执行，如果不写则请求终止
        Request request = chain.request();//获取请求
        Response response = chain.proceed(request);//获取最终响应,如果链接出现重定向，则可能出现响应链接和请求链接不一致的情况
        String requestResult = logRequest(response.networkResponse().request());//直接使用request无法获取系统的Header，所以从response里面获取request。
//        QLKPatientApplication.base_log.i("该次请求的链接",""+request.url()+"\n=====>(请求的参数)"+requestResult);
        logResponse(response);
        return response;
    }
    /**
     * 判断数据类型是否为文本类型
     * @param mediaType
     * @return
     */
    private boolean isText(MediaType mediaType)
    {
        //原先这里把数据类型打印出来，但是容易出现空指针错误
        if (mediaType.type() != null && mediaType.type().equals("text"))
        {
            return true;
        }
        if (mediaType.subtype() != null)
        {
            if (mediaType.subtype().equals("json") ||
                    mediaType.subtype().equals("xml") ||
                    mediaType.subtype().equals("html") ||
                    mediaType.subtype().equals("webviewhtml") ||
                    //application/x-www-form-urlencoded是采用form形式进行上传参数的一种方式，该参数展示在Content-Type里面
                    // 如果是Get方式会追加在url后面，一般是 ? 后面(?可以后台修改，比如用分号 ; 区分)
                    // 如果是POST方式会填充在body里面。
                    // application/x-www-form-urlencoded类型不能上传文件，如果上传文件的话需要使用form的另一种类型multipart/form-data
                    mediaType.subtype().equals("x-www-form-urlencoded")
                    )
                return true;
        }
        return false;
    }
    private String logRequest(final Request request){
        if (null == request){
//            QLKPatientApplication.base_log.e("该次请求异常请仔细检查请求链接或者确认服务器是否运行良好");
            return "网络异常，请检查链接";
        }
        Headers requestHeaders = request.headers();
        int length = requestHeaders.size();
        for (int i = 0; i < length; i++){
            String requestHeaderName = requestHeaders.name(i);
            String requestHeaderValue = requestHeaders.value(i);
//            Log.i("该次请求的Header","Name:"+requestHeaderName+"  Value:"+requestHeaderValue);
        }
        RequestBody requestBody = request.body();
        if (requestBody != null)
        {
            MediaType mediaType = requestBody.contentType();
            if (mediaType != null)
            {
                return bodyToString(request);
//                if (isText(mediaType))
//                {
//                    return bodyToString(request);
//                } else {
//                    return "上传类型不为文本类型";
//                }
            }
        }
        return "无法获取请求的内容";
    }

    private Response logResponse(final Response response) throws IOException {
        ResponseBody responseBody = response.body();
        Charset charset = Charset.forName("UTF-8");
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();
        MediaType contentType = responseBody.contentType();
        if (null != contentType){
            if (isText(responseBody.contentType())){
                charset = contentType.charset(Charset.defaultCharset());
                String responseResult = buffer.clone().readString(charset);
//                QLKPatientApplication.base_log.i("该次响应的链接",""+response.request().url()+"\n=====>类型为:"+responseBody.contentType()+"\n=====>(返回的结果)"+responseResult);
                return response.newBuilder().build();
            }else {
//                QLKPatientApplication.base_log.i("该次响应的链接",""+response.request().url()+"=====>(返回的结果不为文本类型)类型为:"+responseBody.contentType());
            }
        }
        return response;
    }

    private String bodyToString(final Request request)
    {
        try
        {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e)
        {
            return "something error when show requestBody.";
        }
    }
}