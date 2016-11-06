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

package org.microg.gms.droidguard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RemoteDroidGuardConnector {
    private static final String TAG = "GmsDroidGuardConn";

    private Context context;

    public RemoteDroidGuardConnector(Context context) {
        this.context = context;
    }

    public Result guard(final String type, final String androidIdLong) {
        return guard(type, androidIdLong, new Bundle());
    }

    public synchronized Result guard(final String type, final String androidIdLong, final Bundle extras) {
        final Result res = new Result();
        res.statusCode = 14;
        connectForTask(new Task() {
            @Override
            public void run(IRemoteDroidGuard remote, final CountDownLatch countDownLatch) {
                try {
                    RemoteDroidGuardRequest request = new RemoteDroidGuardRequest();
                    request.packageName = context.getPackageName();
                    request.reason = type;
                    request.androidIdLong = androidIdLong;
                    request.extras = extras;
                    remote.guard(new IRemoteDroidGuardCallback.Stub() {
                        @Override
                        public void onResult(byte[] result) throws RemoteException {
                            res.result = result;
                            res.statusCode = 0;
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onError(String err) throws RemoteException {
                            res.statusCode = 8;
                            res.errorMsg = err;
                            countDownLatch.countDown();
                        }
                    }, request);
                } catch (RemoteException e) {
                    Log.w(TAG, e);
                    res.statusCode = 8;
                    countDownLatch.countDown();
                }
            }
        });
        return res;
    }

    @Deprecated
    public Result guard(String type, String androidIdLong, Bundle extras, String id1, String id2) {
        return guard(type, androidIdLong, extras);
    }

    private boolean connectForTask(Task todo) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Intent intent = new Intent("org.microg.gms.droidguard.REMOTE");
        intent.setPackage("org.microg.gms.droidguard");
        boolean b = context.bindService(intent, new Connection(countDownLatch, todo), Context.BIND_AUTO_CREATE);
        if (!b) return false;
        try {
            countDownLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return true;
    }

    private interface Task {
        void run(IRemoteDroidGuard remote, CountDownLatch countDownLatch);
    }

    private class Connection implements ServiceConnection {

        private CountDownLatch countDownLatch;
        private Task todo;

        public Connection(CountDownLatch countDownLatch, Task todo) {
            this.countDownLatch = countDownLatch;
            this.todo = todo;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                if (todo != null) {
                    todo.run(IRemoteDroidGuard.Stub.asInterface(service), countDownLatch);
                }
                todo = null;
            } catch (Exception e) {
                context.unbindService(this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            countDownLatch.countDown();
        }
    }

    public class Result {
        private byte[] result;
        private int statusCode;
        private String errorMsg;

        public byte[] getResult() {
            return result;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getErrorMsg() {
            return errorMsg;
        }
    }
}
