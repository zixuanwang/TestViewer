#include <jni.h>
#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <opencv2/opencv.hpp>
#include <sys/time.h>
#include <time.h>
#include <sstream>
#include <fstream>
#include <pthread.h>
#include <list>
#include "Sensor.h"
#include "PlanarObjectTracker.h"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "jni_native", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "jni_native", __VA_ARGS__))

extern "C" {

std::list<std::pair<jlong, cv::Mat> > frameQueue;
pthread_t id;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
PlanarObjectTracker* pTracker;
bool sInitTrack = false;
bool sTrackSuccess = false;

void* consumer(void* arg) {
	while (true) {
		std::pair < jlong, cv::Mat > framePair(0, cv::Mat());
		pthread_mutex_lock(&mutex);
		while (frameQueue.empty()) {
			pthread_cond_wait(&cond, &mutex);
		}
		framePair = frameQueue.front();
		frameQueue.pop_front();
		pthread_mutex_unlock(&mutex);
		if (!framePair.second.empty()) {
			std::stringstream ss;
			ss << "/mnt/sdcard/tmp/" << framePair.first << ".pgm";
			cv::imwrite(ss.str(), framePair.second);
			LOGI("frame saved");
		}
		if (framePair.first != 0) {
			std::ofstream outStream;
			outStream.open("/mnt/sdcard/tmp/image.txt", std::ios::app);
			if (outStream.good()) {
				outStream << framePair.first << std::endl;
				outStream.close();
			}
		}
	}
}

void* frameSaver(void *ptr) {
	while (true) {
		std::pair < jlong, cv::Mat > framePair(0, cv::Mat());
		pthread_mutex_lock(&mutex);
		if (!frameQueue.empty()) {
			framePair = frameQueue.front();
			frameQueue.pop_front();
		}
		pthread_mutex_unlock(&mutex);
		if (!framePair.second.empty()) {
			std::stringstream ss;
			ss << "/mnt/sdcard/tmp/" << framePair.first << ".pgm";
			cv::imwrite(ss.str(), framePair.second);
			LOGI("frame saved");
		} else {
			timespec ts;
			ts.tv_nsec = 1000000 * 10;
			ts.tv_sec = 0;
			nanosleep(&ts, 0);
		}
		if (framePair.first != 0) {
			std::ofstream outStream;
			outStream.open("/mnt/sdcard/tmp/image.txt", std::ios::app);
			if (outStream.good()) {
				outStream << framePair.first << std::endl;
				outStream.close();
			}
		}
	}
}

void detectFeature(std::vector<cv::KeyPoint>* pKeypointArray,
		const cv::Mat& image) {
//	cv::FastFeatureDetector detector(50);
//	detector.detect(image, *pKeypointArray);
//	cv::Ptr < cv::FeatureDetector > pDetector = cv::FeatureDetector::create(
//			"ORB");
//	pDetector->detect(image, *pKeypointArray);
//	cv::Ptr < cv::DescriptorExtractor > pDescriptor =
//			cv::DescriptorExtractor::create("ORB");
//	cv::Mat descritpors;
//	pDescriptor->compute(image, *pKeypointArray, descritpors);
	cv::ORB orb(50);
	cv::Mat descriptors;
	orb(image, cv::Mat(), *pKeypointArray, cv::noArray(), false);
	std::stringstream ss;
	ss << "features " << pKeypointArray->size();
	LOGI(ss.str().c_str());
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_MainActivity_startSensor(JNIEnv* env, jobject thiz) {
	startSensor();
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_MainActivity_stopSensor(JNIEnv* env, jobject thiz) {
	stopSensor();
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_MainActivity_startFrameSaver(JNIEnv* env, jobject thiz) {
	pthread_create( &id, NULL, consumer, NULL);
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_MainActivity_stopFrameSaver(JNIEnv* env, jobject thiz) {
	pthread_kill(id,SIGQUIT);
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_MainActivity_processFrame(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jlong timestamp) {
	jbyte* _yuv = env->GetByteArrayElements(yuv, 0);
	cv::Mat grayMat(height, width, CV_8UC1, (unsigned char *)_yuv);
	cv::Mat image = grayMat.clone();
	std::pair <jlong, cv::Mat> framePair(timestamp, image);
	pthread_mutex_lock(&mutex);
	frameQueue.push_back(framePair);
	pthread_cond_signal(&cond);
	pthread_mutex_unlock(&mutex);
	env->ReleaseByteArrayElements(yuv, _yuv, 0);
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_ARView_processFrame(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra, jlong timestamp)
{
	jbyte* _yuv = env->GetByteArrayElements(yuv, 0);
	jint* _bgra = env->GetIntArrayElements(bgra, 0);
	cv::Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
	cv::Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
	cv::Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);
	cv::cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
	if(!pTracker->status()){
		LOGI("Recognizing");
		pTracker->initTrack(mgray);
	}else{
		LOGI("Tracking");
		pTracker->track(mgray);
	}
	if(pTracker->status()){
		LOGI("Recognizing success");
		pTracker->drawProjectedCorners(mbgra);
	}
	env->ReleaseIntArrayElements(bgra, _bgra, 0);
	env->ReleaseByteArrayElements(yuv, _yuv, 0);
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_ARView_startTracker(JNIEnv* env, jobject thiz) {
	pTracker=new PlanarObjectTracker();
	double cameraArray[9]={829.8522751820323f, 0.0f, 364.1695626434915f, 0.0f, 834.2332219739386f, 212.0796690739651f, 0.0f, 0.0f, 1.0f};
	cv::Mat cameraMatrix(3,3,CV_64FC1,cameraArray);
	pTracker->setIntrinsicMatrix(cameraMatrix);
	double distCoeffsArray[5]={0.1765873585831874f, 1.243640848210139f, -0.003312721493263714f, -0.008323445728838329f, -20.13254028524939f};
	cv::Mat distCoeffs(1,5,CV_64FC1,distCoeffsArray);
	pTracker->setDistCoeffs(distCoeffs);
	pTracker->loadTemplate("/mnt/sdcard/learning_python4e.jpg");
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_ARView_stopTracker(JNIEnv* env, jobject thiz) {
	delete pTracker;
}

JNIEXPORT void JNICALL Java_com_microsoft_testviewer_ARView_initTrack(JNIEnv* env, jobject thiz) {
	sInitTrack = true;
}

}
