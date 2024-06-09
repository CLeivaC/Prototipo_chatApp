package com.leiva.prototipo_chatapp.Notificaciones

// Clase para encapsular un token utilizado para notificaciones
class Token {

    // Variable privada que almacena el valor del token
    private var token: String = ""

    // Constructor por defecto
    constructor()

    // Constructor que inicializa el token con un valor específico
    constructor(token: String) {
        this.token = token
    }

    // Método para obtener el valor del token
    fun getToken(): String? {
        return token
    }

    // Método para establecer el valor del token
    fun setToken(token: String?) {
        this.token = token!!
    }
}