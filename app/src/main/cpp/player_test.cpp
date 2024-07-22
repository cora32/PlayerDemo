#include <jni.h>
#include <android/log.h>
#include <string.h>

#define  LOG_TAG    "native_md"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define BUFFER_SIZE 1024 * 32
//#define SAMPLE_SIZE 32768
//#define SAMPLE_SIZE 16384
//#define SAMPLE_SIZE 4096
#define SAMPLE_SIZE 2048
#define BITMAP_HEIGHT SAMPLE_SIZE / 4

#include <iostream>
#include <complex>

using namespace std;

int log2(int N)    /*function to calculate the log2(.) of int numbers*/
{
    int k = N, i = 0;
    while (k) {
        k >>= 1;
        i++;
    }
    return i - 1;
}

int check(int n)    //checking if the number of element is a power of 2
{
    return n > 0 && (n & (n - 1)) == 0;
}

int reverse(int N, int n)    //calculating revers number
{
    int j, p = 0;
    for (j = 1; j <= log2(N); j++) {
        if (n & (1 << (log2(N) - j)))
            p |= 1 << (j - 1);
    }
    return p;
}

void ordina(complex<double> *f1, int N) //using the reverse order in the array
{
    complex<double> f2[SAMPLE_SIZE];
    for (int i = 0; i < N; i++)
        f2[i] = f1[reverse(N, i)];
    for (int j = 0; j < N; j++)
        f1[j] = f2[j];
}

void transform(complex<double> *f, int N) //
{
    ordina(f, N);    //first: reverse order
    complex<double> *W;
    W = (complex<double> *) malloc(N / 2 * sizeof(complex<double>));
    W[1] = polar(1., -2. * M_PI / N);
    W[0] = 1;
    for (int i = 2; i < N / 2; i++)
        W[i] = pow(W[1], i);
    int n = 1;
    int a = N / 2;
    for (int j = 0; j < log2(N); j++) {
        for (int i = 0; i < N; i++) {
            if (!(i & n)) {
                complex<double> temp = f[i];
                complex<double> Temp = W[(i * a) % (n * a)] * f[i + n];
                f[i] = temp + Temp;
                f[i + n] = temp - Temp;
            }
        }
        n *= 2;
        a = a / 2;
    }
    free(W);
}

void FFT(complex<double> *f, int N, double d) {
    transform(f, N);
    for (int i = 0; i < N; i++)
        f[i] *= d; //multiplying by step
}

void process(JNIEnv *env,
             jbyte *src,
             jintArray *pixels,
             int length,
             int *bitmap_column_index,
             int base_color,
             int bitmap_width);

extern "C"
JNIEXPORT jobject JNICALL
Java_io_iskopasi_player_1test_utils_FullSampleExtractor_getWavSpectrum(JNIEnv *env, jobject thiz,
                                                            jobject extractor,
                                                            jobject recvBuffer,
                                                                       int baseColor,
                                                                       jlong fileSize) {
    int bitmap_width = fileSize / SAMPLE_SIZE;
//    int bitmap_width = 3891183 / SAMPLE_SIZE;

    // extractor methods
    jclass extractorClass = env->GetObjectClass(extractor);
    jmethodID readSampleData = env->GetMethodID(extractorClass,
                                                "readSampleData",
                                                "(Ljava/nio/ByteBuffer;I)I");
    jmethodID advanceMethod = env->GetMethodID(extractorClass,
                                               "advance",
                                               "()Z");

    // bytebuffer methods
    jclass byteBufferClass = env->GetObjectClass(recvBuffer);
    jmethodID limitMethod = env->GetMethodID(byteBufferClass,
                                             "limit",
                                             "()I");
    jmethodID positionMethod = env->GetMethodID(byteBufferClass,
                                                "position",
                                                "()I");
    jmethodID putMethod = env->GetMethodID(byteBufferClass,
                                           "put",
                                           "([BII)Ljava/nio/ByteBuffer;");

    // Bitmap methods
    jclass bitmapConfig = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID rgba8888FieldID = env->GetStaticFieldID(bitmapConfig, "ARGB_8888",
                                                     "Landroid/graphics/Bitmap$Config;");
    jobject rgba8888Obj = env->GetStaticObjectField(bitmapConfig, rgba8888FieldID);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethodID = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jmethodID setPixelMethod = env->GetMethodID(bitmapClass,
                                                "setPixel",
                                                "(III)V");
    jobject bitmapObj = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID,
                                                    bitmap_width,
                                                    BITMAP_HEIGHT,
                                                    rgba8888Obj);

    jintArray pixels = env->NewIntArray(bitmap_width * BITMAP_HEIGHT + 1);

//    auto *allBuffer = new jbyte[529200];
//    jobject result = env->NewDirectByteBuffer(allBuffer, 529200);

    // buffer
    auto *recv_buff = (jbyte *) env->GetDirectBufferAddress(recvBuffer);
    auto *sample_buffer = new jbyte[SAMPLE_SIZE]{0};
    auto *cache_buffer = new jbyte[BUFFER_SIZE]{0};
    int size = 0;
    int prev_start = 0;
    int start = 0;
    int x = 0;
    int cache_size = 0;
    int processed = 0;

    LOGE("Bitmap size: %d; width: %d; height: %d", bitmap_width * BITMAP_HEIGHT, bitmap_width,
         BITMAP_HEIGHT);

    memset(cache_buffer, 0, BUFFER_SIZE);
    memset(sample_buffer, 0, SAMPLE_SIZE);

    do {
        // Read data into cache_buffer
        int read = env->CallIntMethod(extractor, readSampleData, recvBuffer, 0);

        // Couldn't read data, advance extractor
        if (read == -1) {
            continue;
        }

        memcpy(cache_buffer + cache_size, recv_buff, read);

        cache_size += read;
        size += cache_size;

        LOGE("cache_size: %d; size: %d", cache_size, size);

        // If not enough data, receive more
        if (cache_size < SAMPLE_SIZE) {
            continue;
        }

        // Slice buffer by SAMPLE_SIZE
        processed = 0;
        for (int i = 0; i < cache_size; i += SAMPLE_SIZE) {
            int remaining = cache_size - processed;
            int new_length = remaining > SAMPLE_SIZE ? SAMPLE_SIZE : remaining;
            memcpy(sample_buffer, cache_buffer + i, new_length);

            process(env,
                    sample_buffer,
                    &pixels,
                    new_length,
                    &x,
                    baseColor,
                    bitmap_width);

            processed += new_length;
        }

        // Process remaining bytes
        if (cache_size - processed > 0) {
            int remaining = cache_size - processed;
            LOGE("Remains: %d bytes", remaining);
            memcpy(sample_buffer, cache_buffer + processed, remaining);

            process(env,
                    sample_buffer,
                    &pixels,
                    remaining,
                    &x,
                    baseColor,
                    bitmap_width);
        }

        memset(cache_buffer, 0, BUFFER_SIZE);
        memset(sample_buffer, 0, SAMPLE_SIZE);

        LOGE("Processed: %d", processed);
        cache_size = 0;
    } while (env->CallBooleanMethod(extractor, advanceMethod));

    LOGE("Complete size: %d", size);

    // Process remaining bytes
    int remaining = cache_size - processed;
    if (remaining > 0) {
        LOGE("Remains: %d bytes", remaining);
        memcpy(sample_buffer, cache_buffer + processed, remaining);

        process(env,
                sample_buffer,
                &pixels,
                remaining,
                &x,
                baseColor,
                bitmap_width);
    }

    // Setting pixels to bitmap
    jmethodID setPixelsMid = env->GetMethodID(bitmapClass, "setPixels", "([IIIIIII)V");
    env->CallVoidMethod(bitmapObj, setPixelsMid, pixels, 0, bitmap_width, 0, 0, bitmap_width,
                        BITMAP_HEIGHT);

    return bitmapObj;
}
extern "C"
JNIEXPORT jobject JNICALL
Java_io_iskopasi_player_1test_utils_FullSampleExtractor_getSpectrumFromDecoded(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jobject recvBuffer,
                                                                               int baseColor,
                                                                               jlong fileSize) {
    int bitmap_width = fileSize / SAMPLE_SIZE;

    // Bitmap methods
    jclass bitmapConfig = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID rgba8888FieldID = env->GetStaticFieldID(bitmapConfig, "ARGB_8888",
                                                     "Landroid/graphics/Bitmap$Config;");
    jobject rgba8888Obj = env->GetStaticObjectField(bitmapConfig, rgba8888FieldID);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethodID = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");

    jmethodID setPixelMethod = env->GetMethodID(bitmapClass,
                                                "setPixel",
                                                "(III)V");
    jobject bitmapObj = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID,
                                                    bitmap_width,
                                                    BITMAP_HEIGHT,
                                                    rgba8888Obj);

    jintArray pixels = env->NewIntArray(bitmap_width * BITMAP_HEIGHT + 1);

//    auto *allBuffer = new jbyte[529200];
//    jobject result = env->NewDirectByteBuffer(allBuffer, 529200);

    // buffer
    auto *recv_buff = (jbyte *) env->GetDirectBufferAddress(recvBuffer);
    auto *sample_buffer = new jbyte[SAMPLE_SIZE]{0};
    int size = 0;
    int prev_start = 0;
    int start = 0;
    int x = 0;
    int processed = 0;

    LOGE("Bitmap size: %d; width: %d; height: %d", bitmap_width * BITMAP_HEIGHT, bitmap_width,
         BITMAP_HEIGHT);

    // Slice buffer by SAMPLE_SIZE
    processed = 0;
    for (int i = 0; i < fileSize; i += SAMPLE_SIZE) {
        int remaining = fileSize - processed;
        int new_length = remaining > SAMPLE_SIZE ? SAMPLE_SIZE : remaining;
        memcpy(sample_buffer, recv_buff + i, new_length);

        process(env,
                sample_buffer,
                &pixels,
                new_length,
                &x,
                baseColor,
                bitmap_width);

        processed += new_length;
    }

    // Process remaining bytes
    if (fileSize - processed > 0) {
        int remaining = fileSize - processed;
        LOGE("Remains: %d bytes", remaining);
        memcpy(sample_buffer, recv_buff + processed, remaining);

        process(env,
                sample_buffer,
                &pixels,
                remaining,
                &x,
                baseColor,
                bitmap_width);
    }

    // Setting pixels to bitmap
    jmethodID setPixelsMid = env->GetMethodID(bitmapClass, "setPixels", "([IIIIIII)V");
    env->CallVoidMethod(bitmapObj, setPixelsMid, pixels, 0, bitmap_width, 0, 0, bitmap_width,
                        BITMAP_HEIGHT);

    return bitmapObj;
}

void process(JNIEnv *env,
             jbyte *src,
             jintArray *pixels,
             int length,
             int *x,
             int base_color,
             int bitmap_width
) {
    complex<double> vec[SAMPLE_SIZE];

    // Converting shorts to floats
    for (int i = 0; i < length; i += 2) {
        double data_in_channel = (src[i] & 0x00ff) |
                                 (src[i + 1] << 8);
        vec[i] = (double) data_in_channel;
    }

    // FFTing
    FFT(vec, SAMPLE_SIZE, 1);

    // Receiving amplitudes
    auto fft_vec_length = SAMPLE_SIZE / 2;
    double amplitudes[fft_vec_length];
    double maxAmplitude = 0;

    for (int i = 0; i < fft_vec_length; ++i) {
        double r = pow(vec[i].real(), 2.0);
        double im = pow(vec[i].imag(), 2.0);
        double amplitude = sqrt(r + im);
//        LOGE("amplitude: %f", amplitude);

        if (amplitude > maxAmplitude) {
            maxAmplitude = amplitude;
        }

        amplitudes[i] = amplitude;
    }

    // Generating bitmap
    float valueFactor = 255 / maxAmplitude;
    for (int y = 0; y < BITMAP_HEIGHT; y++) {
        int alpha = amplitudes[y + BITMAP_HEIGHT] * valueFactor;
        int current_pixel = (base_color & 0x00ffffff) | (alpha << 24);
//        LOGE("current_pixel: %d %d", amplitudes[y + BITMAP_HEIGHT], alpha);

        env->SetIntArrayRegion(*pixels,
                               *x + (y * bitmap_width),
                               1,
                               &current_pixel);
    }

//    LOGE("bitmap_width: %d x: %d", bitmap_width, *x);
    (*x)++;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_io_iskopasi_player_1test_utils_FFTPlayer_fft(JNIEnv *env, jobject thiz,
                                                  jfloatArray src,
                                                  jfloatArray dst) {
    // may be later?..
}