/*
 * Sensor.cpp
 *
 *  Created on: Jul 19, 2012
 *      Author: zixuanwang
 */

#include "Sensor.h"

void startSensor() {
	ALooper* looper = ALooper_forThread();
	if (looper == NULL) {
		looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
	}
	engine.sensorManager = ASensorManager_getInstance();
	// get sensor
//	engine.accelerometerSensor = ASensorManager_getDefaultSensor(
//			engine.sensorManager, ASENSOR_TYPE_ACCELEROMETER);
	engine.gyroscopeSensor = ASensorManager_getDefaultSensor(
			engine.sensorManager, ASENSOR_TYPE_GYROSCOPE);
//	engine.magneticSensor = ASensorManager_getDefaultSensor(
//			engine.sensorManager, ASENSOR_TYPE_MAGNETIC_FIELD);
//	engine.accelerometerEventQueue = ASensorManager_createEventQueue(
//			engine.sensorManager, looper, LOOPER_ID_USER_ACCELEROMETER,
//			accelerometerCallback, &engine);
	engine.gyroscopeEventQueue = ASensorManager_createEventQueue(
			engine.sensorManager, looper, LOOPER_ID_USER_GYROSCOPE,
			gyroscopeCallback, &engine);
//	engine.magneticEventQueue = ASensorManager_createEventQueue(
//			engine.sensorManager, looper, LOOPER_ID_USER_MAGNETIC,
//			magneticCallback, &engine);
	// enable sensor
//	int a = ASensor_getMinDelay(engine.accelerometerSensor);
//	int b = ASensor_getMinDelay(engine.gyroscopeSensor);
//	int c = ASensor_getMinDelay(engine.magneticSensor);
//	LOGI("min-delay: %d, %d, %d", a, b, c);
//	ASensorEventQueue_setEventRate(engine.accelerometerEventQueue,
//			engine.accelerometerSensor, 1000);
	ASensorEventQueue_setEventRate(engine.gyroscopeEventQueue,
			engine.gyroscopeSensor, 100);
//	ASensorEventQueue_setEventRate(engine.magneticEventQueue,
//			engine.magneticSensor, 1000);
//	ASensorEventQueue_enableSensor(engine.accelerometerEventQueue,
//			engine.accelerometerSensor);
	ASensorEventQueue_enableSensor(engine.gyroscopeEventQueue,
			engine.gyroscopeSensor);
//	ASensorEventQueue_enableSensor(engine.magneticEventQueue,
//			engine.magneticSensor);
}

void stopSensor() {
//	if (engine.accelerometerSensor != NULL) {
//		ASensorEventQueue_disableSensor(engine.accelerometerEventQueue,
//				engine.accelerometerSensor);
//	}
	if (engine.gyroscopeSensor != NULL) {
		ASensorEventQueue_disableSensor(engine.gyroscopeEventQueue,
				engine.gyroscopeSensor);
	}
//	if (engine.magneticSensor != NULL) {
//		ASensorEventQueue_disableSensor(engine.magneticEventQueue,
//				engine.magneticSensor);
//	}
}

int accelerometerCallback(int fd, int events, void* data) {
	struct engine_t* userData = (struct engine_t*) data;
	ASensorEvent event[NB_SENSOR_EVENTS];
	int i, n;
	while ((n = ASensorEventQueue_getEvents(userData->accelerometerEventQueue,
			event, NB_SENSOR_EVENTS)) > 0) {
		for (i = 0; i < n; ++i) {
			ASensorVector* vector = &event[i].vector;
			if (event[i].type == ASENSOR_TYPE_ACCELEROMETER) {
				std::stringstream ss;
//				ss << std::setprecision(16)
//						<< (double) event[i].timestamp / 1000000;
				ss << vector->x << "\t" << vector->y << "\t" << vector->z;
				accelerometerLogger.save(ss.str());
			}
		}
	}
	return 1;
}

int gyroscopeCallback(int fd, int events, void* data) {
	struct engine_t* userData = (struct engine_t*) data;
	ASensorEvent event[NB_SENSOR_EVENTS];
	int i, n;
	while ((n = ASensorEventQueue_getEvents(userData->gyroscopeEventQueue,
			event, NB_SENSOR_EVENTS)) > 0) {
		for (i = 0; i < n; ++i) {
			ASensorVector* vector = &event[i].vector;
			if (event[i].type == ASENSOR_TYPE_GYROSCOPE) {
				std::stringstream ss;
				ss << std::setprecision(32)
						<< (double) event[i].timestamp / 1000000 << "\t";
				ss << vector->x << "\t" << vector->y << "\t" << vector->z;
				gyroscopeLogger.save(ss.str());
			}
		}
	}
	return 1;
}

int magneticCallback(int fd, int events, void* data) {
	struct engine_t* userData = (struct engine_t*) data;
	ASensorEvent event[NB_SENSOR_EVENTS];
	int i, n;
	while ((n = ASensorEventQueue_getEvents(userData->magneticEventQueue, event,
			NB_SENSOR_EVENTS)) > 0) {
		for (i = 0; i < n; ++i) {
			ASensorVector* vector = &event[i].vector;
			if (event[i].type == ASENSOR_TYPE_MAGNETIC_FIELD) {
				std::stringstream ss;
//				ss << std::setprecision(16)
//						<< (double) event[i].timestamp / 1000000;
				ss << vector->x << "\t" << vector->y << "\t" << vector->z;
				magneticLogger.save(ss.str());
			}
		}
	}
	return 1;
}
