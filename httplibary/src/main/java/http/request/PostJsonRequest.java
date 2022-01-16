package http.request;

import java.util.Map;

import http.Exceptions;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
/**
 * post请求Json
 * create by 马杨茗 on 2019/11/22
 * @version 1.0
 */
public class PostJsonRequest extends OkHttpRequest{
    private static MediaType MEDIA_TYPE_PLAIN = MediaType.parse("application/json; charset=utf-8");

    private String content;
    private MediaType mediaType;
    public PostJsonRequest(String url, Object tag, Map<String, String> params, Map<String, String> headers, String content, MediaType mediaType, int id)
    {
        super(url, tag, params, headers,id);
        this.content = content;
        this.mediaType = mediaType;

        if (this.content == null)
        {
            Exceptions.illegalArgument("the content can not be null !");
        }
        if (this.mediaType == null)
        {
            this.mediaType = MEDIA_TYPE_PLAIN;
        }

    }
    @Override
    protected RequestBody buildRequestBody() {
        return RequestBody.create(content,mediaType);
    }

    @Override
    protected Request buildRequest(RequestBody requestBody) {
        return builder.post(requestBody).build();
    }
}