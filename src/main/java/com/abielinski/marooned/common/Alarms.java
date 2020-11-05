/*******************************************************************************
 * This file is part of Marooned.
 *
 * Marooned is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Marooned is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Marooned.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.abielinski.marooned.common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.abielinski.marooned.Receivers.NewMessageChecker;
import com.abielinski.marooned.Receivers.RegularCachePruner;

import java.util.HashMap;
import java.util.Map;

public class Alarms {
	private static Map<Alarm, AlarmManager> alarmMap = new HashMap<>();
	private static Map<Alarm, PendingIntent> intentMap = new HashMap<>();

	/*
		An enum to represent an alarm that may be created.
		If you wish to add an alarm, just add it at the top of the enum with the 3 arguments,
		and then call startAlarm() on it.
	 */

	public enum Alarm {
		MESSAGE_CHECKER(AlarmManager.INTERVAL_HALF_HOUR, NewMessageChecker.class, true),
		CACHE_PRUNER(AlarmManager.INTERVAL_HOUR, RegularCachePruner.class, true);

		private final long interval;
		private final Class alarmClass;
		private final boolean startOnBoot;

		Alarm(final long interval, final Class alarmClass, final boolean startOnBoot) {
			this.interval = interval;
			this.alarmClass = alarmClass;
			this.startOnBoot = startOnBoot;
		}

		private long interval() {
			return interval;
		}

		private Class alarmClass() {
			return alarmClass;
		}

		private boolean startOnBoot() {
			return startOnBoot;
		}
	}

	/**
	 * Starts the specified alarm
	 *
	 * @param alarm   alarm to start
	 * @param context
	 */

	public static void startAlarm(final Alarm alarm, final Context context) {
		if(!alarmMap.containsKey(alarm)) {
			final Intent alarmIntent = new Intent(context, alarm.alarmClass());
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(
					context,
					0,
					alarmIntent,
					0);

			final AlarmManager alarmManager
					= (AlarmManager)(context.getSystemService(Context.ALARM_SERVICE));
			alarmManager.setInexactRepeating(
					AlarmManager.RTC,
					System.currentTimeMillis(),
					alarm.interval(),
					pendingIntent);

			alarmMap.put(alarm, alarmManager);
			intentMap.put(alarm, pendingIntent);
		}
	}

	/**
	 * Stops the specified alarm
	 *
	 * @param alarm alarm to stop
	 */

	public static void stopAlarm(final Alarm alarm) {
		if(alarmMap.containsKey(alarm)) {
			alarmMap.get(alarm).cancel(intentMap.get(alarm));
			alarmMap.remove(alarm);
			intentMap.remove(alarm);
		}
	}

	/**
	 * Starts all alarms that are supposed to start at device boot
	 *
	 * @param context
	 */

	public static void onBoot(final Context context) {
		for(final Alarm alarm : Alarm.values()) {
			if(alarm.startOnBoot()) {
				startAlarm(alarm, context);
			}
		}
	}
}
