#include <jni.h>
#include <windows.h>

JNIEXPORT jbyteArray JNICALL Java_de_isuret_polos_AetherOnePi_hotbits_hrng_HrngGenerator_generateRandomBytes(JNIEnv *env, jobject obj, jint numBytes) {
    HCRYPTPROV hCryptProv;
    jbyteArray result = NULL;

    if (!CryptAcquireContext(&hCryptProv, NULL, NULL, PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)) {
        return NULL;
    }

    BYTE* buffer = (BYTE*)malloc(numBytes);
    if (buffer == NULL) {
        CryptReleaseContext(hCryptProv, 0);
        return NULL;
    }

    if (!CryptGenRandom(hCryptProv, numBytes, buffer)) {
        free(buffer);
        CryptReleaseContext(hCryptProv, 0);
        return NULL;
    }

    result = (*env)->NewByteArray(env, numBytes);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, numBytes, (jbyte*)buffer);
    }

    free(buffer);
    CryptReleaseContext(hCryptProv, 0);

    return result;
}
