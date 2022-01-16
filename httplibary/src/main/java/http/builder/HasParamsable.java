package http.builder;

import java.util.Map;

/**
 * OkHttp参数配置接口
 * @author 马杨茗 date 2016/7/4
 * @version 1.0
 */
public interface HasParamsable{
    OkHttpRequestBuilder params(Map<String, String> params);
    OkHttpRequestBuilder addParams(String key, String val);
}