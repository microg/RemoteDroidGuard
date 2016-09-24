package org.microg.gms.droidguard;

interface IRemoteDroidGuardCallback {
    void onResult(in byte[] result) = 0;
    void onError(String msg) = 1;
}