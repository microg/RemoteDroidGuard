package org.microg.gms.droidguard;

import org.microg.gms.droidguard.IRemoteDroidGuardCallback;

interface IRemoteDroidGuard {
    void guard(IRemoteDroidGuardCallback callback, String packageName, String type, in Bundle data);
}