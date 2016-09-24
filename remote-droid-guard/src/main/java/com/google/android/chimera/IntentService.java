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

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public abstract class IntentService extends Service {

    public IntentService(Context base) {
        super(base);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {

    }

    public void onDestroy() {

    }

    public abstract void onHandleIntent(Intent intent);

    public void onStart(Intent intent, int i) {

    }

    public int onStartCommand(Intent intent, int a, int b) {
        return 0;
    }

    public void setIntentRedelivery(boolean bool) {

    }
}
