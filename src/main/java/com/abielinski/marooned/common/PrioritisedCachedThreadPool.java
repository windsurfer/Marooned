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

import android.util.Log;

import java.util.ArrayList;

public class PrioritisedCachedThreadPool {

	private final ArrayList<Task> mTasks = new ArrayList<>(16);
	private final Executor mExecutor = new Executor();

	private final int mMaxThreads;
	private final String mThreadName;
	private int mThreadNameCount = 0;

	private int mRunningThreads, mIdleThreads;

	public PrioritisedCachedThreadPool(final int threads, final String threadName) {
		mMaxThreads = threads;
		mThreadName = threadName;
	}

	public void add(final Task task) {

		synchronized(mTasks) {
			mTasks.add(task);

			if(mIdleThreads < 1 && mRunningThreads < mMaxThreads) {
				mRunningThreads++;
				//Log.d("Executor", "Starting another thread with " + mIdleThreads + " idle threads and " + mRunningThreads + " running");
				new Thread(mExecutor, mThreadName + " " + (mThreadNameCount++)).start();
			}else {
				//Log.d("Executor", "No need to start a thread because there are " + mIdleThreads + " idle threads and " + mRunningThreads + " running");
			}
			//Log.d("Executor", "Added a task, now there are " + mTasks.size());
			mTasks.notifyAll();
		}
	}

	public static abstract class Task {

		public boolean isHigherPriorityThan(final Task o) {
			return getPrimaryPriority() < o.getPrimaryPriority()
					|| getSecondaryPriority() < o.getSecondaryPriority();
		}

		public abstract int getPrimaryPriority();

		public abstract int getSecondaryPriority();

		public abstract void run();
	}

	private final class Executor implements Runnable {

		@Override
		public void run() {

			while(true) {

				Task taskToRun = null;

				synchronized(mTasks) {

					if(mTasks.isEmpty()) {

						mIdleThreads++;

						try {
							//Log.d("Executor", "Waiting for more work with " + mIdleThreads + " idle threads");
							mTasks.wait(15000);
						} catch(final InterruptedException e) {
							throw new RuntimeException(e);
						} finally {
							mIdleThreads--;
						}

						if(mTasks.isEmpty()) {
							int oldRunning = mRunningThreads;
							mRunningThreads--;
							//Log.d("Executor", "Quitting thread (from " + oldRunning + ") leaving only " + mRunningThreads + " running");
							return;
						}
					}

					int taskIndex = -1;
					for(int i = 0; i < mTasks.size(); i++) {
						if(taskToRun == null || mTasks.get(i)
								.isHigherPriorityThan(taskToRun)) {
							taskToRun = mTasks.get(i);
							taskIndex = i;
						}
					}
					if (taskIndex == -1){
						Log.w("Executor", "Something happened to our task!");
					}
					mTasks.remove(taskIndex);
				}

				assert taskToRun != null;
				taskToRun.run();
			}
		}
	}
}
