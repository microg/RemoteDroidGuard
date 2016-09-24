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

import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import de.larma.arthook.$;
import de.larma.arthook.ArtHook;
import de.larma.arthook.BackupIdentifier;
import de.larma.arthook.Hook;
import de.larma.arthook.OriginalMethod;

public class SysHook {
    private static final String TAG = "SysHook";

    private static boolean done = false;
    private static String odexArch = "arm"; // or "arm64"
    private static String checksum;

    // From TelephonyManager
    private static String deviceId;
    private static String subscriberId;

    public synchronized static void activate(String odexArch, String checksum, String deviceId, String subscriberId) {
        SysHook.odexArch = odexArch;
        SysHook.checksum = checksum;
        SysHook.deviceId = deviceId;
        SysHook.subscriberId = subscriberId;
        if (done) return;
        done = true;
        ArtHook.hook(SysHook.class);
    }

    @Hook("android.telephony.TelephonyManager->getSubscriberId")
    public static String TelephonyManager_getSubscriberId(TelephonyManager tm) {
        return subscriberId;
    }

    @Hook("android.telephony.TelephonyManager->getDeviceId")
    public static String TelephonyManager_getDeviceId(TelephonyManager tm) {
        return deviceId;
    }

    @Hook("android.telephony.TelephonyManager->getDeviceId")
    public static String TelephonyManager_getDeviceId(TelephonyManager tm, int slot) {
        return deviceId;
    }

    @Hook("android.content.ContextWrapper->getPackageName")
    public static String ContextWrapper_getPackageName(Object o) {
        return "com.google.android.gms";
    }

    @Hook("android.content.ContextWrapper->getClassLoader")
    public static ClassLoader ContextWrapper_getClassLoader(Object o) {
        return new URLClassLoader(new URL[0], (ClassLoader) OriginalMethod.by(new $() {
        }).invoke(o)) {
            @Override
            public String toString() {
                return "dalvik.system.PathClassLoader[DexPathList[[zip file \"/system/framework/com.android.location.provider.jar\", zip file \"/system/framework/com.android.media.remotedisplay.jar\", zip file \"/data/app/com.google.android.gms-1/base.apk\"],nativeLibraryDirectories=[/data/app/com.google.android.gms-1/lib/arm, /data/app/com.google.android.gms-1/base.apk!/lib/armeabi-v7a, /vendor/lib, /system/lib]]]";
            }
        };
    }

    @Hook("java.util.TreeSet->iterator")
    @BackupIdentifier("TreeSetIterator")
    public static Iterator TreeSet_iterator(TreeSet set) {
        OriginalMethod originalMethod = OriginalMethod.by("TreeSetIterator");
        Iterator iterator = originalMethod.invoke(set);
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (!(o instanceof String)) {
                break;
            }
            String s = (String) o;
            if (s.contains("org.microg.gms") && s.contains(".apk")) {
                Log.d(TAG, "Replaced TreeSet with specially designed version");
                // The special case
                Set<String> replacement = new TreeSet<>();
                replacement.add("/data/app/com.google.android.gms-1/base.apk");
                replacement.add("/data/app/com.google.android.gms-1/oat/" + odexArch + "/base.odex");
                replacement.add("/data/dalvik-cache/" + odexArch + "/system@framework@com.android.location.provider.jar@classes.dex");
                replacement.add("/data/dalvik-cache/" + odexArch + "/system@framework@com.android.media.remotedisplay.jar@classes.dex");
                replacement.add("/data/data/com.google.android.gms/app_dg_cache/" + checksum.toUpperCase() + "/opt/the.dex");
                replacement.add("/data/data/com.google.android.gms/app_fb/f.dex (deleted)");
                replacement.add("/system/framework/com.android.location.provider.jar");
                replacement.add("/system/framework/com.android.media.remotedisplay.jar");
                replacement.add("/system/framework/framework-res.apk");
                return originalMethod.invoke(replacement);
            }
        }
        return originalMethod.invoke(set);
    }
}
