/*
 * Sensor.h
 *
 *  Created on: Jul 19, 2012
 *      Author: zixuanwang
 */

#ifndef SENSOR_H_
#define SENSOR_H_

#include <jni.h>
#include <errno.h>
#include <android/sensor.h>
#include <android/log.h>
#include <android_native_app_glue.h>
#include <sstream>
#include <iomanip>
#include "Logger.h"

// user-defined looper ids
#define LOOPER_ID_USER_ACCELEROMETER (LOOPER_ID_USER + 0)
#define LOOPER_ID_USER_GYROSCOPE (LOOPER_ID_USER + 1)
#define LOOPER_ID_USER_MAGNETIC (LOOPER_ID_USER + 2)
#define NB_SENSOR_EVENTS 8

struct engine_t {
	ASensorManager* sensorManager;
	const ASensor* accelerometerSensor;
	const ASensor* gyroscopeSensor;
	const ASensor* magneticSensor;
	ASensorEventQueue* sensorEventQueue;
	ASensorEventQueue* accelerometerEventQueue;
	ASensorEventQueue* gyroscopeEventQueue;
	ASensorEventQueue* magneticEventQueue;
};

static struct engine_t engine;

static Logger accelerometerLogger("/mnt/sdcard/acc.txt", false);
static Logger gyroscopeLogger("/mnt/sdcard/gyro.txt", false);
static Logger magneticLogger("/mnt/sdcard/mag.txt", false);

void startSensor();
void stopSensor();

// sensor callback functions
int accelerometerCallback(int fd, int events, void* data);
int gyroscopeCallback(int fd, int events, void* data);
int magneticCallback(int fd, int events, void* data);

#endif /* SENSOR_H_ */
