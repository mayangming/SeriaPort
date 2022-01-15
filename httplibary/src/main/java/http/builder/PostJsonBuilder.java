package http.builder;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import http.request.PostJsonRequest;
import http.request.RequestCall;
import okhttp3.MediaType;

public class PostJsonBuilder extends OkHttpRequestBuilder<PostJsonBuilder>{
    private String content;
    private MediaType mediaType;
    private Map<String,String> params = new HashMap<>();
    public PostJsonBuilder content(String json)
    {
        if (!params.isEmpty()){
            throw new RuntimeException("addParams()函数和content()只能使用一种");
        }
        this.content = json;
        return this;
    }

    public PostJsonBuilder addParams(Map<String,String> params)
    {
        if (!TextUtils.isEmpty(content)){
            throw new RuntimeException("addParams()函数和content()只能使用一种");
        }
        this.params.putAll(params);
        return this;
    }

    public PostJsonBuilder addParams(String key,String value){
        if (!TextUtils.isEmpty(content)){
            throw new RuntimeException("addParams()函数和content()只能使用一种");
        }
        params.put(key,value);
        return this;
    }

    public PostJsonBuilder mediaType(MediaType mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public RequestCall build() {
        if (TextUtils.isEmpty(content)){
            content = params2Json(params);
        }
        return new PostJsonRequest(url, tag, params, headers, content, mediaType,id).build();
    }
    private String params2Json(Map<String,String> params){
        String content = "{}";
        try {
            JSONObject object = new JSONObject();
            for(Map.Entry<String, String> entry : params.entrySet()){
                String mapKey = entry.getKey();
                String mapValue = entry.getValue();
                object.put(mapKey,mapValue);
            }
            content = object.toString();
        }catch (JSONException e){
            e.printStackTrace();
        }

        return content;
    }
}
