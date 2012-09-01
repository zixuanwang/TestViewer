LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
OPENCV_LIB_TYPE:=STATIC
OPENCV_INSTALL_MODULES:=on
OPENCV_MK_PATH=/cygdrive/c/lib/OpenCV-2.4.2-android-sdk/sdk/native/jni/OpenCV.mk
include $(OPENCV_MK_PATH)
LOCAL_MODULE    := native_sample
LOCAL_SRC_FILES := Logger.cpp jni_native.cpp Sensor.cpp PlanarObjectTracker.cpp SE3_basic.cpp ImageResizer.cpp GeometricVerifier.cpp
LOCAL_LDLIBS    += -llog -ldl -landroid -lEGL -lGLESv1_CM
LOCAL_STATIC_LIBRARIES += android_native_app_glue
include $(BUILD_SHARED_LIBRARY)
$(call import-module, android/native_app_glue)
