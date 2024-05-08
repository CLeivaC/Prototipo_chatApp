package com.leiva.prototipo_chatapp.Notificaciones;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
        "Content-Type:application/json",
        "Authorization:key=AAAAEvRZc-M:APA91bFeYAyfOCGjGWPhAvDlRuZhB5VvTkijtbzBAuwbA8vU27w_86fnuQ3Jr3dUvqOWz0sqQaoddfw0wJzvw0jFdajY93qrVeU5KT62L705h0uL-FGcz3_iVwzJ7nQLoNzZ7I8dL1F9"
    })

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
