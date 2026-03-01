package com.phonecluster.app

object AsconNative {
    init { System.loadLibrary("asconjni") }

    external fun encrypt(
        key: ByteArray,
        nonce: ByteArray,
        ad: ByteArray?,
        plaintext: ByteArray
    ): ByteArray?

    external fun decrypt(
        key: ByteArray,
        nonce: ByteArray,
        ad: ByteArray?,
        ciphertext: ByteArray
    ): ByteArray?
}