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

package com.abielinski.marooned;

import android.app.Application;
import android.util.Log;
import com.abielinski.marooned.cache.CacheManager;
import com.abielinski.marooned.common.Alarms;
import com.abielinski.marooned.io.RedditChangeDataIO;
import com.abielinski.marooned.Receivers.NewMessageChecker;
import com.abielinski.marooned.Reddit.prepared.RedditChangeDataManager;

public class Marooned extends Application {

	@Override
	public void onCreate() {

		super.onCreate();

		Log.i("Marooned", "Application created.");

		final CacheManager cm = CacheManager.getInstance(this);

		new Thread() {
			@Override
			public void run() {

				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

				cm.pruneTemp();
				cm.pruneCache();
			}
		}.start();

		new Thread() {
			@Override
			public void run() {
				RedditChangeDataIO.getInstance(Marooned.this)
						.runInitialReadInThisThread();
				RedditChangeDataManager.pruneAllUsers(Marooned.this);
			}
		}.start();

		Alarms.onBoot(this);

		NewMessageChecker.checkForNewMessages(this);
	}
}
