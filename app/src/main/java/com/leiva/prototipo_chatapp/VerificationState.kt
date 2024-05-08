package com.leiva.prototipo_chatapp

class VerificationState private constructor() {

    companion object {
        private var storedVerificationId: String? = null

        fun getStoredVerificationId(): String? {
            return storedVerificationId
        }

        fun setStoredVerificationId(id: String) {
            storedVerificationId = id
        }
    }
}