#include <jni.h>
#include <android/log.h>
#include <string.h>

#define  LOG_TAG    "native_md"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define SAMPLE_SIZE 4096
#define BITMAP_HEIGHT SAMPLE_SIZE / 4

#include <iostream>
#include <complex>

using namespace std;

//#define M_PI 3.1415926535897932384

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
             int bitmap_width,
             jobject bitmapObj,
             jmethodID setPixelMethod);

extern "C"
JNIEXPORT jobject JNICALL
Java_io_iskopasi_player_1test_utils_FullSampleExtractor_fft(JNIEnv *env, jobject thiz,
                                                            jobject extractor,
                                                            jobject recvBuffer,
                                                            int base_color) {
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
//    jmethodID putMethod = env->GetMethodID(byteBufferClass,
//                                           "put",
//                                           "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;");
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
    int bitmap_width = 529200 / SAMPLE_SIZE + 1;
    jobject bitmapObj = env->CallStaticObjectMethod(bitmapClass, createBitmapMethodID,
                                                    bitmap_width, BITMAP_HEIGHT, rgba8888Obj);

    jintArray pixels = env->NewIntArray(bitmap_width * BITMAP_HEIGHT);

//    auto *allBuffer = new jbyte[529200];
//    jobject result = env->NewDirectByteBuffer(allBuffer, 529200);

    // buffer
    auto *recvBuff = (jbyte *) env->GetDirectBufferAddress(recvBuffer);
    auto *sampleBuffer = new jbyte[SAMPLE_SIZE];
    int size = 0;
    int prevStart = 0;
    int start = 0;
    int x = 0;

    do {
        // Read data into recvBuff
        int lSize = env->CallIntMethod(extractor, readSampleData, recvBuffer, 0);
        size += lSize;

        LOGE("READ: %d %d", lSize, size);
        // Slice big chunk
        if (lSize > SAMPLE_SIZE) {
            int remains = lSize;
            start = 0;

            // Slice buffer by SAMPLE_SIZE
            do {
                LOGE("start: %d", start);
                int length = SAMPLE_SIZE - prevStart;

                memcpy(sampleBuffer + prevStart, recvBuff + start, length);
                process(env,
                        sampleBuffer,
                        &pixels,
                        length,
                        &x,
                        base_color,
                        bitmap_width,
                        bitmapObj,
                        setPixelMethod);

                remains -= length;
                start += length;
                prevStart = 0;
            } while (remains >= SAMPLE_SIZE);

            LOGE(" Processed %d; %d remains", start, remains);

            // There is a remaining data, save position to process it on next iteration
            if (remains > 0) {
                LOGE("remaining: %d", remains);

                memcpy(sampleBuffer, recvBuff + start, remains);
                process(env,
                        sampleBuffer,
                        &pixels,
                        remains,
                        &x,
                        base_color,
                        bitmap_width,
                        bitmapObj,
                        setPixelMethod);

                prevStart = remains;
            }
        } else {
            // Accumulate small chunks

        }
    } while (env->CallBooleanMethod(extractor, advanceMethod));

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
             int bitmap_width,
             jobject bitmapObj,
             jmethodID setPixelMethod
) {
    complex<double> vec[SAMPLE_SIZE];

    // Converting shorts to floats
    for (int i = 0; i < length; i += 2) {
        double data_in_channel = (src[i] & 0x00ff) |
                                 (src[i + 1] << 8);
//        auto float_value = (double)data_in_channel / 32768.0;

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

        if (amplitude > maxAmplitude) {
            maxAmplitude = amplitude;
        }

        amplitudes[i] = amplitude;
    }

    // Generating bitmap
    float valueFactor = 255 / maxAmplitude;
    for (int y = BITMAP_HEIGHT - 1; y >= 0; y--) {
        int alpha = amplitudes[y + BITMAP_HEIGHT] * valueFactor;
        int current_pixel = (base_color & 0x00ffffff) | (alpha << 24);

        env->SetIntArrayRegion(*pixels,
                               *x + (y * bitmap_width),
                               1,
                               &current_pixel);

//        env->CallVoidMethod(bitmapObj, setPixelMethod, *x, y, current_pixel);
    }

    (*x)++;
}
