package http.builder;

import http.request.PostFileRequest;
import http.request.RequestCall;

import java.io.File;

import okhttp3.MediaType;

/**
 * 通过post提交文件的辅助类
 * @author 马杨茗 date 2016/7/7
 * @version 1.0
 */
public class PostFileBuilder extends OkHttpRequestBuilder<PostFileBuilder>
{
    private File file;
    private MediaType mediaType;//网络传输过程中的数据类型


    public OkHttpRequestBuilder file(File file)
    {
        this.file = file;
        return this;
    }

    public OkHttpRequestBuilder mediaType(MediaType mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public RequestCall build()
    {
        return new PostFileRequest(url, tag, params, headers, file, mediaType,id).build();
    }


}
