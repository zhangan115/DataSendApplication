package com.example.yanfa.myapplication.mode.netWork;

import com.example.yanfa.myapplication.BuildConfig;
import com.example.yanfa.myapplication.app.DataApp;
import com.example.yanfa.myapplication.mode.Bean;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.protobuf.ProtoConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;


public class Api {
    //测试服务器
    public static final String HOST = "http://127.0.0.1:16011/SITO4000/";

    private static Retrofit mRetrofit;
    private static final int CONNECT_TIME = 5;
    private static final int READ_TIME = 200;
    private static final int WRITE_TIME = 200;

    public static Retrofit createRetrofit() {
        if (mRetrofit == null) {
            initRetrofit();
        }
        return mRetrofit;
    }

    public static void clean(){
        mRetrofit=null;
    }

    private static void initRetrofit() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIME, TimeUnit.SECONDS)
                .readTimeout(READ_TIME, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIME, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        String host = DataApp.getInstance().AppHost();
        if (!host.endsWith("/")) {
            host = host + "/";
        }
        mRetrofit = new Retrofit.Builder()
                .client(builder.build())
                .baseUrl(host)
                .addConverterFactory(ProtoConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    public interface UploadData {

        //上传图片
        @POST("mobile/uploadImg.json")
        @Multipart
        Observable<Bean<String>> postImageFile(@Part List<MultipartBody.Part> partList);

        //上传超声波接口
        @Headers({"Content-Type:application/json;charset=utf-8", "Accept:application/json;"})
        @POST("mobile/uploadUltrasonicData.json")
        Observable<Bean<String>> uploadUltrasonicData(@Body() String info);

        //上传高频数据
        @Headers({"Content-Type:application/json;charset=utf-8", "Accept:application/json;"})
        @POST("mobile/uploadUhfData.json")
        Observable<Bean<String>> uploadUhfData(@Body() String info);

        //上传红外数据
        @Headers({"Content-Type:application/json;charset=utf-8", "Accept:application/json;"})
        @POST("mobile/uploadInfrareData.json")
        Observable<Bean<String>> uploadInfrareData(@Body() String info);

    }

}
