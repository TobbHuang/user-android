package com.huangtao.user.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Network {

    private static Api api;

    public static Api getInstance() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    //设置数据解析器
                    .addConverterFactory(GsonConverterFactory.create())
                    //设置网络请求的Url地址
                    .baseUrl("http://129.211.119.51:31000/")
//                    .baseUrl("http://192.168.0.102:8080/")
                    .build();
            // 创建网络请求接口的实例
            api = retrofit.create(Api.class);
        }
        return api;
    }

}
