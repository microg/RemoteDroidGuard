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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.microg.gms.droidguard.Constants.GMS_PACKAGE_NAME;

public class DalvikHook {
    private static final String TAG = "DalvikHook";

    static boolean done = false;
    static String odexArch = "arm"; // or "arm64"
    static String checksum;

    // From TelephonyManager
    static String deviceId;
    static String subscriberId;

    public synchronized static void activate(String odexArch, String checksum, String deviceId, String subscriberId) {
        DalvikHook.odexArch = odexArch;
        DalvikHook.checksum = checksum;
        DalvikHook.deviceId = deviceId;
        DalvikHook.subscriberId = subscriberId;
        if (done) return;
        done = true;
        System.loadLibrary("dalvikhook");
    }

    //Hook android.telephony.TelephonyManager->getSubscriberId
    public static String TelephonyManager_getSubscriberId(TelephonyManager tm) {
        Log.d(TAG, "Set subscriber id: " + DalvikHook.subscriberId);
        return subscriberId;
    }

    //Hook android.telephony.TelephonyManager->getDeviceId
    public static String TelephonyManager_getDeviceId(TelephonyManager tm) {
        Log.d(TAG, "Set device id: " + DalvikHook.deviceId);
        return deviceId;
    }

    //Hook android.telephony.TelephonyManager->getDeviceId
    public static String TelephonyManager_getDeviceId(TelephonyManager tm, int slot) {
        Log.d(TAG, "Set device id 2: " + DalvikHook.deviceId);
        return deviceId;
    }

    //Hook android.net.ConnectivityManager->getActiveNetworkInfo
    public static NetworkInfo ConnectivityManager_getActiveNetworkInfo(ConnectivityManager cm) {
        return null; // null is a valid response
    }

    //Hook java.util.regex.Pattern->matcher
    public static Matcher Pattern_matcher(Pattern pattern, CharSequence cs) {
        Log.d(TAG, "pettern_matcher called");
        if (cs instanceof String) {
            String s = (String) cs;
            Log.d(TAG, s);
            if (s.contains("Xposed") || s.contains("xposed") || s.contains("XPosed") || s.contains("XPOSED"))
                return DalvikHook.Pattern_matcher(pattern, "");
            if (s.contains("org.microg.gms.droidguard"))
                return DalvikHook.Pattern_matcher(pattern, s.replace("org.microg.gms.droidguard", "com.google.android.gms"));
        }
        return DalvikHook.Pattern_matcher(pattern, cs);
    }

    //Hook android.content.ContextWrapper->getPackageName
    public static String ContextWrapper_getPackageName(Object o) {
        Log.d(TAG, "Set package name: " + GMS_PACKAGE_NAME);
        return GMS_PACKAGE_NAME;
    }

    //Hook android.os.SystemProperties->getInt
    public static int SystemProperties_getInt(String key, int def) {
        if (key.equals("ro.debuggable") || key.equals("service.adb.root"))
            return 0;
        if (key.equals("ro.secure"))
            return 1;
        return DalvikHook.SystemProperties_getInt(key, def);
    }

    //Hook java.util.Arrays->asList
    public static List Arrays_asList(Object[] objs) {
        Log.d(TAG, "Arrays->asList: "+Arrays.toString(objs));
        if (detectLibrariesList(objs))
            return DalvikHook.Arrays_asList(new Object[]{getModifiedSystemSharedLibraries()});
        return DalvikHook.Arrays_asList(new Object[]{objs});
    }

    static boolean detectLibrariesList(Object[] list) {
        boolean isList = false;
        boolean wasModified = false;
        for (int i = 0; i < list.length; i++) {
            if (!(list[i] instanceof String))
                return false;
            if ("com.android.location.provider".equals(list[i]) || "android.test.runner".equals(list[i]))
                isList = true;
            if ("com.google.widevine.software.drm".equals(list[i]))
                wasModified = true;
        }
        return isList && !wasModified;
    }

    //Hook android.app.ApplicationPackageManager->getSystemSharedLibraryNames
    public static String[] PackageManager_getSystemSharedLibraryNames(Object o) {
        Log.d(TAG, "Modified SystemSharedLibraries");
        return getModifiedSystemSharedLibraries();
    }

    static String[] getModifiedSystemSharedLibraries() {
        return new String[]{"android.test.runner",
                "com.android.future.usb.accessory",
                "com.android.location.provider",
                "com.android.media.remotedisplay",
                "com.android.mediadrm.signer",
                "com.google.android.maps",
                "com.google.widevine.software.drm",
                "com.qualcomm.qcrilhook",
                "com.qualcomm.qti.rcsservice",
                "com.quicinc.cneapiclient",
                "javax.obex",
                "org.apache.http.legacy"};
    }

    //Hook android.content.ContextWrapper->getClassLoader
    public static ClassLoader ContextWrapper_getClassLoader(Object o) {
        return createModifiedClassLoader((ClassLoader) DalvikHook.ContextWrapper_getClassLoader(o));
    }

    static ClassLoader createModifiedClassLoader(ClassLoader original) {
        return new URLClassLoader(new URL[0], original) {
            @Override
            public String toString() {
                return "dalvik.system.PathClassLoader[DexPathList[[zip file \"/system/framework/com.android.location.provider.jar\", zip file \"/system/framework/com.android.media.remotedisplay.jar\", zip file \"/data/app/com.google.android.gms-1/base.apk\"],nativeLibraryDirectories=[/data/app/com.google.android.gms-1/lib/arm, /data/app/com.google.android.gms-1/base.apk!/lib/armeabi-v7a, /vendor/lib, /system/lib]]]";
            }
        };
    }

    //Hook java.util.TreeSet->iterator
    public static Iterator TreeSet_iterator(TreeSet set) {
        if (detectMapsSet((Iterator) DalvikHook.TreeSet_iterator(set)))
            return DalvikHook.TreeSet_iterator((TreeSet) createMapsReplacementSet());
        return DalvikHook.TreeSet_iterator(set);
    }

    static boolean detectMapsSet(Iterator iterator) {
        boolean hasDgCache = false;
        boolean hasFrameworkRes = false;
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (!(o instanceof String)) {
                return false;
            }
            String s = (String) o;
            Log.d(TAG, "mapdetect: " + s);
            if (s.contains("app_dg_cache")) hasDgCache = true;
            if (s.contains("framework-res.apk")) hasFrameworkRes = true;
        }
        return hasDgCache && hasFrameworkRes;
    }

    static Set<String> createMapsReplacementSet() {
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
        return replacement;
    }
}
