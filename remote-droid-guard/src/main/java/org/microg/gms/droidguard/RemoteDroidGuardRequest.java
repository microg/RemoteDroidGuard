/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import android.os.Bundle;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class RemoteDroidGuardRequest extends AutoSafeParcelable {

    // should match caller
    @SafeParceled(1)
    public String packageName;

    // Where to put this DroidGuard response, known values: "attest", "fast"
    @SafeParceled(2)
    public String reason;

    // From GSettings store
    @SafeParceled(3)
    public String androidIdLong;

    // additional fields, known key: "contentBinding"
    @SafeParceled(100)
    public Bundle extras;

    public RemoteDroidGuardRequest() {

    }

    public static final Creator<RemoteDroidGuardRequest> CREATOR = new AutoCreator<>(RemoteDroidGuardRequest.class);
}
