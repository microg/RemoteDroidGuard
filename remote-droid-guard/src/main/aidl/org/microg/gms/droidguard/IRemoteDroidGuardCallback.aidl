/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

interface IRemoteDroidGuardCallback {
    void onResult(in byte[] result) = 0;
    void onError(String msg) = 1;
}
