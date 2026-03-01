#include <jni.h>
#include <stdlib.h>
#include "crypto_aead.h"
#include "api.h"
#include "ascon.h"

// Encrypt
JNIEXPORT jbyteArray JNICALL
Java_com_phonecluster_app_AsconNative_encrypt(
        JNIEnv* env, jclass clazz,
        jbyteArray key,
        jbyteArray nonce,
        jbyteArray ad,
        jbyteArray plaintext) {

    (void)clazz;

    if (!key || (*env)->GetArrayLength(env, key) != CRYPTO_KEYBYTES) return NULL;
    if (!nonce || (*env)->GetArrayLength(env, nonce) != CRYPTO_NPUBBYTES) return NULL;
    if (!plaintext) return NULL;

    jsize adLen = ad ? (*env)->GetArrayLength(env, ad) : 0;
    jsize mLen  = (*env)->GetArrayLength(env, plaintext);

    jbyte* kBytes  = (*env)->GetByteArrayElements(env, key, NULL);
    jbyte* nBytes  = (*env)->GetByteArrayElements(env, nonce, NULL);
    jbyte* adBytes = ad ? (*env)->GetByteArrayElements(env, ad, NULL) : NULL;
    jbyte* mBytes  = (*env)->GetByteArrayElements(env, plaintext, NULL);

    unsigned long long cLenULL = 0;
    unsigned char* c = (unsigned char*)malloc((size_t)mLen + CRYPTO_ABYTES);
    if (!c) return NULL;

    crypto_aead_encrypt(
            c, &cLenULL,
            (const unsigned char*)mBytes, (unsigned long long)mLen,
            (const unsigned char*)adBytes, (unsigned long long)adLen,
            NULL,
            (const unsigned char*)nBytes,
            (const unsigned char*)kBytes
    );

    jbyteArray out = (*env)->NewByteArray(env, (jsize)cLenULL);
    (*env)->SetByteArrayRegion(env, out, 0, (jsize)cLenULL, (const jbyte*)c);

    free(c);
    (*env)->ReleaseByteArrayElements(env, plaintext, mBytes, JNI_ABORT);
    if (adBytes) (*env)->ReleaseByteArrayElements(env, ad, adBytes, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, nonce, nBytes, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, kBytes, JNI_ABORT);

    return out;
}

// Decrypt
JNIEXPORT jbyteArray JNICALL
Java_com_phonecluster_app_AsconNative_decrypt(
        JNIEnv* env, jclass clazz,
        jbyteArray key,
        jbyteArray nonce,
        jbyteArray ad,
        jbyteArray ciphertext) {

    (void)clazz;

    if (!key || (*env)->GetArrayLength(env, key) != CRYPTO_KEYBYTES) return NULL;
    if (!nonce || (*env)->GetArrayLength(env, nonce) != CRYPTO_NPUBBYTES) return NULL;
    if (!ciphertext) return NULL;

    jsize adLen = ad ? (*env)->GetArrayLength(env, ad) : 0;
    jsize cLen  = (*env)->GetArrayLength(env, ciphertext);

    jbyte* kBytes  = (*env)->GetByteArrayElements(env, key, NULL);
    jbyte* nBytes  = (*env)->GetByteArrayElements(env, nonce, NULL);
    jbyte* adBytes = ad ? (*env)->GetByteArrayElements(env, ad, NULL) : NULL;
    jbyte* cBytes  = (*env)->GetByteArrayElements(env, ciphertext, NULL);

    unsigned long long mLenULL = 0;
    unsigned char* m = (unsigned char*)malloc((size_t)cLen);
    if (!m) return NULL;

    int rc = crypto_aead_decrypt(
            m, &mLenULL,
            NULL,
            (const unsigned char*)cBytes, (unsigned long long)cLen,
            (const unsigned char*)adBytes, (unsigned long long)adLen,
            (const unsigned char*)nBytes,
            (const unsigned char*)kBytes
    );

    if (rc != 0) {
        free(m);
        return NULL; // auth failed
    }

    jbyteArray out = (*env)->NewByteArray(env, (jsize)mLenULL);
    (*env)->SetByteArrayRegion(env, out, 0, (jsize)mLenULL, (const jbyte*)m);

    free(m);
    (*env)->ReleaseByteArrayElements(env, ciphertext, cBytes, JNI_ABORT);
    if (adBytes) (*env)->ReleaseByteArrayElements(env, ad, adBytes, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, nonce, nBytes, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, key, kBytes, JNI_ABORT);

    return out;
}