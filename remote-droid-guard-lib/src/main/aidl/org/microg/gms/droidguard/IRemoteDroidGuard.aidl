package org.microg.gms.droidguard;

import org.microg.gms.droidguard.IRemoteDroidGuardCallback;
import org.microg.gms.droidguard.RemoteDroidGuardRequest;

interface IRemoteDroidGuard {
    void guard(IRemoteDroidGuardCallback callbach, in RemoteDroidGuardRequest request) = 0;
}