package jp.ac.hal.androiduberriderremake.Remote;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    private static Retrofit instance;

    public static Retrofit getInstance() {
        return instance == null ? new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/") //Don't forget last/
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build() : instance;
    }
}
