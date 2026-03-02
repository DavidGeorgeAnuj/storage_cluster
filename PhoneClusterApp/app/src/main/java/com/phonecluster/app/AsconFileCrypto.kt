package com.phonecluster.app

import java.io.File
import java.security.SecureRandom

private val rng = SecureRandom()

private val MAGIC = byteArrayOf(
    'A'.code.toByte(), 'S'.code.toByte(), 'C'.code.toByte(), 'N'.code.toByte()
)

private fun randomNonce16(): ByteArray = ByteArray(16).also { rng.nextBytes(it) }

fun encryptFile(input: File, output: File, key: ByteArray) {
    val plaintext = input.readBytes()
    val nonce = randomNonce16()
    val ad: ByteArray? = null

    val ct = AsconNative.encrypt(key, nonce, ad, plaintext)
        ?: throw IllegalStateException("Encryption failed")

    output.parentFile?.mkdirs()
    output.writeBytes(MAGIC + nonce + ct)
}

fun decryptFile(input: File, output: File, key: ByteArray) {
    val all = input.readBytes()
    if (all.size < 4 + 16) throw IllegalArgumentException("File too small")

    val magic = all.copyOfRange(0, 4)
    if (!magic.contentEquals(MAGIC)) throw IllegalArgumentException("Not an ASCON file")

    val nonce = all.copyOfRange(4, 20)
    val ct = all.copyOfRange(20, all.size)
    val ad: ByteArray? = null

    val pt = AsconNative.decrypt(key, nonce, ad, ct)
        ?: throw SecurityException("Auth failed / wrong key / corrupted file")

    output.parentFile?.mkdirs()
    output.writeBytes(pt)
}