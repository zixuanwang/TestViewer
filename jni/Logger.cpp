/*
 * Logger.cpp
 *
 *  Created on: Jul 18, 2012
 *      Author: zixuanwang
 */

#include "Logger.h"

Logger::Logger(const std::string& filepath, bool increment) :
		mIncrement(increment), mLastTime(0.0) {
	mOutStream.open(filepath.c_str());
}

Logger::~Logger() {
	mOutStream.close();
}

void Logger::save(const std::string& message) {
	struct timeval tv;
	gettimeofday(&tv, NULL);
	uint64_t currentTime = tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
	if (mIncrement) {
		if (mLastTime != 0.0) {
			uint64_t gap = currentTime - mLastTime;
			mOutStream << gap << "\t" << message << std::endl;
		}
		mLastTime = currentTime;
	} else {
		mOutStream << currentTime << "\t" << message << std::endl;
	}
}
