/*
 * Copyright (C) 2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.thermal;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ThermalService extends Service {

    private static final String TAG = "ThermalService";
    private static final boolean DEBUG = false;

    private String mPreviousApp;
    private ThermalUtils mThermalUtils;

    private IActivityTaskManager mActivityTaskManager;

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            try {
                final RootTaskInfo info = mActivityTaskManager.getFocusedRootTaskInfo();
                if (info == null || info.topActivity == null) {
                    return;
                }

                String foregroundApp = info.topActivity.getPackageName();
                if (!foregroundApp.equals(mPreviousApp)) {
                    mThermalUtils.setThermalProfile(foregroundApp);
                    mPreviousApp = foregroundApp;
                }
            } catch (Exception e) {
            }
        }
    };

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mPreviousApp = "";
            mThermalUtils.setDefaultThermalProfile();
            mThermalUtils.resetTouchModes();
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating ThermalService");
        mThermalUtils = new ThermalUtils(this);
        try {
            mActivityTaskManager = ActivityTaskManager.getService();
            mActivityTaskManager.registerTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            // Do nothing
        }
        registerReceiver();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying ThermalService");
        unregisterReceiver();
        try {
            ActivityTaskManager.getService().unregisterTaskStackListener(mTaskListener);
        } catch (RemoteException e) {
            // Do nothing
        }
        mThermalUtils.setDefaultThermalProfile();
        mThermalUtils = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting ThermalService");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mThermalUtils.updateTouchRotation();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        this.registerReceiver(mIntentReceiver, filter);
    }

    private void unregisterReceiver() {
        this.unregisterReceiver(mIntentReceiver);
    }
}
