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

import android.content.ContextWrapper;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.larma.arthook.$;
import de.larma.arthook.ArtHook;
import de.larma.arthook.BackupIdentifier;
import de.larma.arthook.Hook;
import de.larma.arthook.OriginalMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static org.microg.gms.droidguard.Constants.GMS_PACKAGE_NAME;

public class SysHook implements IXposedHookLoadPackage {
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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("org.microg.gms.droidguard"))
            return;

        XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return subscriberId;
            }
        });

        XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return deviceId;
            }
        });

        XposedHelpers.findAndHookMethod(TelephonyManager.class, "getDeviceId", int.class, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                return deviceId;
            }
        });

        XposedHelpers.findAndHookMethod(ContextWrapper.class, "getPackageName", XC_MethodReplacement.returnConstant(GMS_PACKAGE_NAME));

        XposedHelpers.findAndHookMethod(ContextWrapper.class, "getClassLoader", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(createModifiedClassLoader((ClassLoader) param.getResult()));
            }
        });

        XposedHelpers.findAndHookMethod(TreeSet.class, "iterator", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                NavigableMap map = (NavigableMap) XposedHelpers.getObjectField(param.thisObject, "m");
                if (map == null)
                    map = (NavigableMap) XposedHelpers.getObjectField(param.thisObject, "backingMap");
                if (map == null)
                    return;
                if (detectMapsSet(map.navigableKeySet().iterator())) {
                    param.setResult(createMapsReplacementSet().iterator());
                }
            }
        });

        SysHook.done = true;
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
        return GMS_PACKAGE_NAME;
    }

    @Hook("android.content.ContextWrapper->getClassLoader")
    @BackupIdentifier("ContextWrapperClassLoader")
    public static ClassLoader ContextWrapper_getClassLoader(Object o) {
        return createModifiedClassLoader((ClassLoader) OriginalMethod.by("ContextWrapperClassLoader").invoke(o));
    }

    private static ClassLoader createModifiedClassLoader(ClassLoader original) {
        return new URLClassLoader(new URL[0], original) {
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
        if (detectMapsSet((Iterator) originalMethod.invoke(set)))
            return originalMethod.invoke(createMapsReplacementSet());
        return originalMethod.invoke(set);
    }

    private static boolean detectMapsSet(Iterator iterator) {
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (!(o instanceof String)) {
                return false;
            }
            String s = (String) o;
            if (s.contains("org.microg.gms") && s.contains(".apk")) {
                Log.d(TAG, "Detected maps set");
                return true;
            }
        }
        return false;
    }

    private static Set<String> createMapsReplacementSet() {
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
