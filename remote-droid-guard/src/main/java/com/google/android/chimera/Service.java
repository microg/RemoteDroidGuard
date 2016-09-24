/*
 * Copyright 2013-2016 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.chimera;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;

public abstract class Service extends ContextWrapper {

    public Service(Context base) {
        super(base);
    }

    public void dump(java.io.FileDescriptor d, java.io.PrintWriter w, java.lang.String[] s) {
    }

    public final Application getApplication() {
        return null;
    }

    public android.app.Service getContainerService() {
        return null;
    }

    public abstract IBinder onBind(Intent intent);

    public void onConfigurationChanged(android.content.res.Configuration configuration) {
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public void onLowMemory() {
    }

    public void onRebind(Intent intent) {
    }

    public void onStart(Intent intent, int i) {
    }

    public int onStartCommand(Intent intent, int a, int b) {
        return 0;
    }

    public void onTaskRemoved(Intent intent) {
    }

    public void onTrimMemory(int i) {
    }

    public boolean onUnbind(Intent intent) {
        return false;
    }

    public void publicDump(java.io.FileDescriptor d, java.io.PrintWriter w, java.lang.String[] s) {
    }

    public void setProxy(android.app.Service service, android.content.Context context) {
    }

    public final void startForeground(int i, Notification n) {
    }

    public final void stopForeground(boolean b) {
    }

    public final void stopSelf() {
    }

    public final void stopSelf(int i) {
    }

    public final boolean stopSelfResult(int i) {
        return false;
    }
}
