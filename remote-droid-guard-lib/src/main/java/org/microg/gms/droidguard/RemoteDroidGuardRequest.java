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
