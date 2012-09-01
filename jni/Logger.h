/*
 * Logger.h
 *
 *  Created on: Jul 18, 2012
 *      Author: zixuanwang
 */

#ifndef LOGGER_H_
#define LOGGER_H_

#include <string>
#include <sys/time.h>
#include <time.h>
#include <fstream>

// this class is not thread safe.
class Logger {
public:
	Logger(const std::string& filepath, bool increment = false);
	~Logger();
	void save(const std::string& message = "");
private:
	std::ofstream mOutStream;
	bool mIncrement;
	uint64_t mLastTime;
};

#endif /* LOGGER_H_ */
