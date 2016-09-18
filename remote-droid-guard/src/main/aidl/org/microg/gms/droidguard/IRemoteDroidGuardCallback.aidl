package org.microg.gms.droidguard;

interface IRemoteDroidGuardCallback {
    void onResult(in byte[] result);
    void onError();
}