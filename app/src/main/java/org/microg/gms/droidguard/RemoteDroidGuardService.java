/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class RemoteDroidGuardService extends Service {
    private static final String TAG = "GmsDroidGuardRemote";

    @Override
    public IBinder onBind(Intent intent) {
        return new IRemoteDroidGuard.Stub() {

            @Override
            public void guard(final IRemoteDroidGuardCallback callback, final RemoteDroidGuardRequest request) throws RemoteException {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onResult(DroidguardHelper.guard(RemoteDroidGuardService.this, request));
                        } catch (Exception e) {
                            Log.w(TAG, e);
                            try {
                                callback.onError(e.getMessage());
                            } catch (RemoteException e1) {
                                stopSelf();
                            }
                        }
                    }
                }).start();
            }
        };
    }
}
