package http.builder;

import java.util.Map;

import http.request.PostStringRequest;
import http.request.RequestCall;
import okhttp3.MediaType;

/**
 * post请求方式的参数配置
 * @author 马杨茗 date 2016/7/5
 * @version 1.0
 */
public class PostStringBuilder extends OkHttpRequestBuilder<PostStringBuilder>
{
    private String content;
    private MediaType mediaType;


    public PostStringBuilder content(String content)
    {
        this.content = content;
        return this;
    }

    public PostStringBuilder addParams(Map<String,String> params)
    {
        this.content = params2Content(params);
        return this;
    }

    public PostStringBuilder mediaType(MediaType mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }



    public RequestCall build()
    {
        return new PostStringRequest(url, tag, params, headers, content, mediaType,id).build();
    }

    private String params2Content(Map<String,String> params){
        String content = "";
        for(Map.Entry<String, String> entry : params.entrySet()){
            String mapKey = entry.getKey();
            String mapValue = entry.getValue();
            System.out.println(mapKey+":"+mapValue);
            content = mapKey+"="+mapValue+"&";
        }
        int lastIndex = content.lastIndexOf("&");
        content = content.substring(0,lastIndex);
        return content;
    }
}
