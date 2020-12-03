package jp.ac.hal.androiduberriderremake.Remote;

import io.reactivex.Observable;
import jp.ac.hal.androiduberriderremake.Model.FCMResponse;
import jp.ac.hal.androiduberriderremake.Model.FCMSendData;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAABaC3YYM:APA91bFKImv7HUdhS0Vj61c-KE7vZbYuFysw8zMJJUjjtkV3UNCEasAoWCXKYxDs6jV2uRN9ocZgqY76eHjy_TUnyOgeo75GUi8zOgKTx_OOpg49kL9BiW828nqcO9S3qfayj-bUqpsn"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
