package com.leiva.prototipo_chatapp.Modelo

class Contacto {
    private var nombre:String = ""
    private var telefono: String = ""

    constructor()

    constructor(
        nombre:String,
        telefono:String
    ){
        this.nombre = nombre
        this.telefono = telefono
    }

    fun getNombre(): String {
        return nombre
    }

    fun setNombre(nombre: String) {
        this.nombre = nombre
    }

    fun getTelefono(): String {
        return telefono
    }

    fun setTelefono(telefono: String) {
        this.telefono = telefono
    }
}