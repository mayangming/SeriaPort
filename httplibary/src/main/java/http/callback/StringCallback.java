package http.callback;

import okhttp3.Response;


/**
 * 返回String数据的回调
 * create by 马杨茗 on 2016/07/04
 * @version 1.0
 */
public abstract class StringCallback extends Callback<String>{
    @Override
    public String parseNetworkResponse(Response response, int id) throws Exception {
        //Response.body().string()方法在调用后会关闭Response.body(),另外string()方法只能打印1M之内的内容,若打印内容超过1M需要使用流进行打印
        String result = response.body().string();
//        if (){//主要用于json返回的状态值判断
//            requestSuccess = true;
//        }else {
//            requestSuccess = false;
//        }
        System.out.println("返回的结果:"+result);
        return result;
    }
}