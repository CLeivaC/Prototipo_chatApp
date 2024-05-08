package com.leiva.prototipo_chatapp.Modelo

class Usuario {

    private var uid: String = ""
    private var n_usuario: String = ""
    private var telefono: String = ""
    private var imagen: String = ""
    private var password: String = ""
    private var apellido: String = ""
    private var infoAdicional: String = ""
    private var nombre: String = ""
    private var pregunta: String = ""
    private var respuesta : String = ""
    private var oculto : Boolean?=null
    private var claroOscuro : Boolean?=null
    private var notificaciones : Boolean?=null

    var ultimoMensajeTimestamp: Long = 0L

    constructor() {

    }
    constructor(
        uid: String,
        n_usuario: String,
        telefono: String,
        imagen: String,
        password: String,
        apellido: String,
        infoAdicional: String,
        nombre: String,
        pregunta:String,
        respuesta:String,
        oculto:Boolean,
        claroOscuro:Boolean,
        notificaciones : Boolean
    ) {
        this.uid = uid
        this.n_usuario = n_usuario
        this.telefono = telefono
        this.imagen = imagen
        this.password = password
        this.apellido = apellido
        this.infoAdicional = infoAdicional
        this.nombre = nombre
        this.pregunta = pregunta
        this.respuesta = respuesta
        this.oculto = oculto
        this.claroOscuro = claroOscuro
        this.notificaciones = notificaciones
    }

    fun getUid(): String {
        return uid
    }

    fun setUid(uid: String) {
        this.uid = uid
    }
    fun getPregunta(): String {
        return pregunta
    }

    fun setPregunta(pregunta: String) {
        this.pregunta = pregunta
    }

    fun getRespuesta(): String {
        return respuesta
    }

    fun setRespuesta(respuesta: String) {
        this.respuesta = respuesta
    }

    fun getN_Usuario(): String {
        return n_usuario
    }

    fun setN_Usuario(n_usuario: String) {
        this.n_usuario = n_usuario
    }

    fun getTelefono(): String {
        return telefono
    }

    fun setTelefono(telefono: String) {
        this.telefono = telefono
    }

    fun getImagen(): String {
        return imagen
    }

    fun setImagen(imagen: String) {
        this.imagen = imagen
    }

    fun getPassword(): String {
        return password
    }

    fun setPassword(password: String) {
        this.password = password
    }

    fun getApellido(): String {
        return apellido
    }

    fun setApellido(apellido: String) {
        this.apellido = apellido
    }

    fun getInfoAdicional(): String {
        return infoAdicional
    }

    fun setInfoAdicional(infoAdicional: String) {
        this.infoAdicional = infoAdicional
    }

    fun getNombre(): String {
        return nombre
    }

    fun setNombre(nombre: String) {
        this.nombre = nombre
    }
    fun getOculto():Boolean?{
        return oculto
    }

    fun setOculto(oculto: Boolean){
        this.oculto = oculto
    }

    fun getClaroOscuro():Boolean?{
        return claroOscuro
    }

    fun setClaroOscuro(claroOscuro: Boolean){
        this.claroOscuro = claroOscuro
    }

    fun getNotificaciones(): Boolean? {
        return notificaciones
    }

    fun setNotificaciones(notificaciones: Boolean) {
        this.notificaciones = notificaciones
    }

}
