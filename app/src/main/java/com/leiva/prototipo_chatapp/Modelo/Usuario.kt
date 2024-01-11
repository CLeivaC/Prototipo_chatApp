package com.leiva.prototipo_chatapp.Modelo

class Usuario {


        private var uid : String = ""
        private var n_usuario : String = ""
        private var telefono : String = ""
        private var imagen : String = ""
        private var password: String = ""

        constructor()

        constructor(
            uid: String,
            n_usuario: String,
            telefono: String,
            imagen: String,
            password: String

        ) {
            this.uid = uid
            this.n_usuario = n_usuario
            this.telefono = telefono
            this.imagen = imagen
            this.password = password
        }

        fun getUid() : String?{
            return uid
        }

        fun setUid(uid : String){
            this.uid = uid
        }

        fun getN_Usuario() : String?{
            return n_usuario
        }

        fun setN_Usuario(n_usuario : String){
            this.n_usuario = n_usuario
        }

        fun getTelefono() : String?{
            return telefono
        }


        fun getImagen() : String?{
            return imagen
        }

        fun setImagen(imagen : String){
            this.imagen = imagen
        }

        fun getPassword():String?{
            return password
        }


    }