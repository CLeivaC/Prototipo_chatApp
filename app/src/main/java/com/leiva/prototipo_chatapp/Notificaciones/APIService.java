package com.leiva.prototipo_chatapp.Notificaciones;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    // Anotación @Headers para definir cabeceras estáticas que se enviarán con cada solicitud
    @Headers({
            // Especifica el tipo de contenido de la solicitud
            "Content-Type:application/json",
            // Autorización con la clave del servidor para la autenticación con el servicio FCM (Firebase Cloud Messaging)
            "Authorization:key=AAAAEvRZc-M:APA91bFeYAyfOCGjGWPhAvDlRuZhB5VvTkijtbzBAuwbA8vU27w_86fnuQ3Jr3dUvqOWz0sqQaoddfw0wJzvw0jFdajY93qrVeU5KT62L705h0uL-FGcz3_iVwzJ7nQLoNzZ7I8dL1F9"
    })

    // Anotación @POST para definir una solicitud POST a la ruta "fcm/send"
    @POST("fcm/send")
    // Método para enviar la notificación, que toma un objeto Sender en el cuerpo de la solicitud
    Call<MyResponse> sendNotification(@Body Sender body);
}
