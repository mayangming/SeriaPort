package http;

import com.ihsanbal.logging.Level;
import com.ihsanbal.logging.LoggingInterceptor;
import com.qlk.httplibary.BuildConfig;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import http.builder.GetBuilder;
import http.builder.PostFormBuilder;
import http.builder.PostJsonBuilder;
import http.builder.PostStringBuilder;
import http.callback.Callback;
import http.log.SimpleInterceptor;
import http.request.RequestCall;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp工具类
 * 此类中静态方法调用之前均需调用初始化方法,初始化方法全局只需一次即可
 * @author 马杨茗 date 2016/7/4
 * @version 1.0
 */
public class OkHttpUtil {
    public static final long DEFAULT_MILLISECONDS = 10_000L;//默认超时时间
    private volatile static OkHttpUtil mInstance;
    private OkHttpClient mOkHttpClient;
    private static Platform mPlatform;//切换到主线程的工具类
    private static Response response;//网络请求的响应类
    /**
     * 该工具类的构造方法
     * @param okHttpClient
     */
    private OkHttpUtil(OkHttpClient okHttpClient, OkHttpClient.Builder builder){
        if (okHttpClient == null)
        {
            mOkHttpClient  = getOkHtpClientBuilder(builder).build();
        } else
        {
            mOkHttpClient = okHttpClient;


        }
        mPlatform = Platform.get();
    }

    /**
     * 对OkHttpClient进行常规配置,如连接超时、读取超时。
     * 当需要对某一请求进行设置改变是，可以使用OkHttpClient.clone()获取实例并进行改变
     * @return OkHttpClient的配置属性
     */
    private OkHttpClient.Builder getOkHtpClientBuilder(OkHttpClient.Builder builder){
        if (null == builder){
            builder = new OkHttpClient.Builder();
        }
        builder.connectTimeout(10, TimeUnit.SECONDS);//链接超时10s
        builder.writeTimeout(5, TimeUnit.SECONDS);//写入超时10s
        builder.readTimeout(5, TimeUnit.SECONDS);//读取超时10s
        builder.addInterceptor(new SimpleInterceptor());
        builder.addInterceptor(getLoggingInterceptor());
        return builder;
    }
    private LoggingInterceptor getLoggingInterceptor(){
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor.Builder()
                .loggable(BuildConfig.DEBUG)
                .setLevel(Level.BASIC)
                .log(okhttp3.internal.platform.Platform.INFO)
                .addHeader("version", "1.0")
                .addQueryParam("query", "0")
                .enableAndroidStudio_v3_LogsHack(true)
//                .enableMock(false, 1000L, request -> {
//                    String segment = request.url().pathSegments().get(0);
//                    return Okio.buffer(Okio.source(ModelConfig.getContext().getAssets().open(String.format("mock/%s.json", segment)))).readUtf8();
//                })
                .executor(Executors.newSingleThreadExecutor())
                .build();
        return loggingInterceptor;
    }
    /**
     * 获取Response以作调试使用
     * @return
     */
    public static Response getResponse(){
        if (null == response)
            Exceptions.illegalArgument("response may be null");
        return response;
    }

    /**
     * 返回get的配置
     * @return
     */
    public static GetBuilder get()
    {
        return new GetBuilder();
    }
    /**
     * 返回post的配置(可用来上传String字符串)
     * @return
     */
    public static PostStringBuilder postString()
    {
        return new PostStringBuilder();
    }
    /**
     * 返回post的配置(可用来进行表单提交)
     * @return
     */
    public static PostFormBuilder postFrom()
    {
        return new PostFormBuilder();
    }
    /**
     * 返回post的配置(可用来进行Json提交)
     * @return
     */
    public static PostJsonBuilder postJson()
    {
        return new PostJsonBuilder();
    }
    /**
     * 一个简单的get请求,该请求不支持属性配置
     * @param url 请求路径
     * @param callback 请求的数据回调
     */
    public static void simpleGet(String url, Callback callback){
        get().url(url).build().execute(callback);
    }
    /**
     * 一个简单的post请求,该请求不支持属性配置
     * @param url 请求路径
     * @param content 上传的字符串
     * @param callback 请求的数据回调
     */
    public static void simplePost(String url, String content, Callback callback){
        postString().url(url).content(content).build().execute(callback);
    }
    /**
     * 获取使用的线程池
     * @return
     */
    public static Executor getExecutor(){
        return mPlatform.defaultCallbackExecutor();
    }

    /**
     * 使用双重锁保证同时只存在一个OkHttpClient实例
     * 该方法目前仅做初始化工作
     * 该方法可以自己对OkHttpClient进行定制,当传null时则使用默认配置的OkHttpClient
     * @param okHttpClient
     * @return
     */
    public static OkHttpUtil initClient(OkHttpClient okHttpClient, OkHttpClient.Builder builder){
        if (mInstance == null)
        {
            synchronized (OkHttpUtil.class)
            {
                if (mInstance == null)
                {
                    mInstance = new OkHttpUtil(okHttpClient,builder);
                }
            }
        }
        return mInstance;
    }

    public static OkHttpUtil initClient(OkHttpClient okHttpClient){
        return initClient(okHttpClient,null);
    }

    /** 可以用来配置Https的请求方式 */
//    public static OkHttpUtil initClient(OkHttpClient.Builder builder){
//        return initClient(null,builder);
//    }

    /**
     * @return 获取OkHttpUtil的单例
     */
    public static OkHttpUtil getInstance(){
        return initClient(null,null);
    }
    /**
     * 尽量使用getInstance()方法以便能够做到随时对网络层的修改
     * @return 获取OkHttpClient的单例
     */
    public OkHttpClient getOkHttpClient(){
        return mOkHttpClient;
    }

    /**
     * 将消息发送到主线程
     * @param request 请求类
     * @param callback 回调
     */
    private void execute(Request request, final Callback callback){
        final int id = 0;
        getOkHttpClient().newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendFailResultCallback(call,e,callback,id);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.body().string()==null){
                    sendSuccessResultCallback("",callback,id);
                }else{
                    sendSuccessResultCallback(response.body().string(),callback,id);
                }

            }
        });
    }
    public void execute(final RequestCall requestCall, Callback callback){
        if (callback == null)
            callback = Callback.CALLBACK_DEFAULT;
        final Callback finalCallback = callback;
        final int id = requestCall.getOkHttpRequest().getId();

        requestCall.getCall().enqueue(new okhttp3.Callback()
        {
            @Override
            public void onFailure(Call call, final IOException e)
            {
                sendFailResultCallback(call, e, finalCallback, id);
            }

            @Override
            public void onResponse(final Call call, final Response response)
            {
                OkHttpUtil.response = response;
                try
                {
                    if (call.isCanceled())
                    {
                        sendFailResultCallback(call, new IOException("Canceled!"), finalCallback, id);
                        return;
                    }

                    if (!finalCallback.validateReponse(response, id))
                    {
                        sendFailResultCallback(call, new IOException("request failed , reponse's code is : " + response.code()), finalCallback, id);
                        return;
                    }

                    Object o = finalCallback.parseNetworkResponse(response, id);
                    sendSuccessResultCallback(o, finalCallback, id);
                } catch (Exception e)
                {
                    sendFailResultCallback(call, e, finalCallback, id);
                } finally
                {
                    if (response.body() != null)
                        response.body().close();
                }

            }
        });
    }
    /**
     * 网络请求失败发送的回调
     * @param call
     * @param e
     * @param callback
     * @param id
     */
    public void sendFailResultCallback(final Call call, final Exception e, final Callback callback, final int id)
    {
        if (callback == null) return;
        mPlatform.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onError(call, e, id);
                callback.onAfter(id);
            }
        });
    }
    /**
     * 网络请求成功发送的回调
     * @param object
     * @param callback
     * @param id
     */
    public void sendSuccessResultCallback(final Object object, final Callback callback, final int id)
    {
        if (callback == null) return;
        mPlatform.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onResponse(object,id);

                callback.onAfter(id);
            }
        });
    }
    /**
     * 取消网络请求，通过tag进行标记
     * @param tag 标记
     */
    public void cancelTag(Object tag)
    {
        for (Call call : mOkHttpClient.dispatcher().queuedCalls())
        {
            if (tag.equals(call.request().tag()))
            {
                call.cancel();
            }
        }
        for (Call call : mOkHttpClient.dispatcher().runningCalls())
        {
            if (tag.equals(call.request().tag()))
            {
                call.cancel();
            }
        }
    }
}