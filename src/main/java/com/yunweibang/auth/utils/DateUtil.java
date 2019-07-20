package com.yunweibang.auth.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

	private static final long TICKS_AT_EPOCH = 116445312000000000L;
	private static final long TICKS_PER_MILLISECOND = 10000;
	private static TimeZone timeZone = TimeZone.getDefault();
	
	public static Date fromDnetToJdate(String str){
		
		if(!"0".equals(str)){
			Calendar calendar = Calendar.getInstance(timeZone);
			calendar.setTimeInMillis((Long.parseLong(str)-TICKS_AT_EPOCH)/TICKS_PER_MILLISECOND);
			calendar.setTimeInMillis(calendar.getTimeInMillis()-calendar.getTimeZone().getRawOffset());
			return calendar.getTime();
		}else {
			return null;
		}
	}
	
}
