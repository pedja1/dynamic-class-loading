#include <jni.h>
#include <stdlib.h>

#include "help/log.h"
#include "xdelta-jni.h"

jint JNI_OnLoad(JavaVM* vm, void* p) {
	JNIEnv* env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		LOGE(TAG, "JavaVM::GetEnv() failed");
		abort();
	}
	registerNativeMethodsNativePatch(env, "com/android_forever/xdelta/XDelta");
	LOGE(TAG, "JNI_OnLoad OKAY");
	return JNI_VERSION_1_6;
}
