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

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.droidguard.DroidGuardChimeraService;
import com.squareup.wire.Wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import okio.ByteString;

public class DroidguardHelper {
    private static final String TAG = "GmsDroidguardHelper";
    private static final String DG_CLASS_NAME = "com.google.ccc.abuse.droidguard.DroidGuard";
    private static final String DG_URL = "https://www.googleapis.com/androidantiabuse/v1/x/create?alt=PROTO&key=AIzaSyBofcZsgLSS7BOnBjZPEkk4rYwzOIz-lTI";
    private static Map<String, Class<?>> loadedClass = new HashMap<>();

    public static byte[] guard(Context context, RemoteDroidGuardRequest request) throws Exception {
        SignedDGResponse signedResponse = request(new DGRequest.Builder()
                .usage(new DGUsage(request.reason, request.packageName))
                .info(getSystemInfo(null))
                .isGoogleCn(false)
                .enableInlineVm(true)
                .currentVersion(3)
                .versionNamePrefix(Constants.GMS_VERSION_NAME_PREFIX)
                .cached(getCached(context))
                .arch(getArch())
                .build());
        DGResponse response = new Wire().parseFrom(signedResponse.data.toByteArray(), DGResponse.class);
        String checksum = response.vmChecksum.hex();
        File dgCacheDir = context.getDir("dg_cache", 0);
        File dgDir = new File(dgCacheDir, checksum);
        dgDir.mkdirs();
        File dgFile = new File(dgDir, "the.apk");
        downloadAndPrepareDg(response, dgCacheDir, dgDir, dgFile);

        Class<?> clazz;
        if (!loadedClass.containsKey(checksum)) {
            ClassLoader loader = new DexClassLoader(new File(dgDir, "the.apk").getAbsolutePath(),
                    new File(dgDir, "opt").getAbsolutePath(), new File(dgDir, "lib").getAbsolutePath(), context.getClassLoader());
            clazz = loader.loadClass(DG_CLASS_NAME);
            loadedClass.put(checksum, clazz);
        } else {
            clazz = loadedClass.get(checksum);
        }

        String odexArch = context.getClassLoader().toString().contains("arm64") ? "arm64" : "arm";
        SysHook.activate(odexArch, checksum, request.deviceId, request.subscriberId);
        return invoke(context, clazz, request.packageName, request.reason, response.byteCode.toByteArray(), request.androidIdLong, request.extras);
    }

    private static Context getSpecialContext(Context context) {
        if (context.getApplicationContext() != null) context = context.getApplicationContext();
        return new DroidGuardChimeraService(context);
    }

    public static byte[] invoke(Context context, Class<?> clazz, String packageName, String type, byte[] byteCode, String androidIdLong, Bundle extras) throws InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException, NoSuchMethodException {
        Object instance = clazz
                .getDeclaredConstructor(Context.class, String.class, byte[].class, Object.class)
                .newInstance(getSpecialContext(context), type, byteCode, new Callback(packageName, androidIdLong));
        final Map<String, String> map = new HashMap<>();
        if (extras != null) {
            for (String key : extras.keySet()) {
                String val = extras.getString(key);
                if (val != null) map.put(key, val);
            }
        }
        return (byte[]) clazz.getMethod("run", Map.class).invoke(instance, map);
    }

    private static List<KeyValuePair> getSystemInfo(Object build) {
        Class clazz = build == null ? Build.class : build.getClass();
        return Arrays.asList(
                createSystemInfoPair("BOARD", clazz, build),
                createSystemInfoPair("BOOTLOADER", clazz, build),
                createSystemInfoPair("BRAND", clazz, build),
                createSystemInfoPair("CPU_ABI", clazz, build),
                createSystemInfoPair("CPU_ABI2", clazz, build),
                createSystemInfoPair("DEVICE", clazz, build),
                createSystemInfoPair("DISPLAY", clazz, build),
                createSystemInfoPair("FINGERPRINT", clazz, build),
                createSystemInfoPair("HARDWARE", clazz, build),
                createSystemInfoPair("HOST", clazz, build),
                createSystemInfoPair("ID", clazz, build),
                createSystemInfoPair("MANUFACTURER", clazz, build),
                createSystemInfoPair("MODEL", clazz, build),
                createSystemInfoPair("PRODUCT", clazz, build),
                createSystemInfoPair("RADIO", clazz, build),
                createSystemInfoPair("SERIAL", clazz, build),
                createSystemInfoPair("TAGS", clazz, build),
                createSystemInfoPair("TIME", clazz, build),
                createSystemInfoPair("TYPE", clazz, build),
                createSystemInfoPair("USER", clazz, build),
                createSystemInfoPair("VERSION.CODENAME", clazz, build),
                createSystemInfoPair("VERSION.INCREMENTAL", clazz, build),
                createSystemInfoPair("VERSION.RELEASE", clazz, build),
                createSystemInfoPair("VERSION.SDK", clazz, build),
                createSystemInfoPair("VERSION.SDK_INT", clazz, build));
    }

    private static KeyValuePair createSystemInfoPair(final String key, final Class clazz, final Object obj) {
        try {
            String[] tr = key.split("\\.");
            Class nuClass = clazz;
            Object nuObj = obj;
            for (int i = 0; i < tr.length - 1; i++) {
                try {
                    Field field = nuClass.getField(tr[i]);
                    nuObj = field.get(nuObj);
                    nuClass = field.getType();
                } catch (NoSuchFieldException ne) {
                    if (obj == null && nuObj == null) {
                        for (Class aClass : nuClass.getDeclaredClasses()) {
                            String name = aClass.getCanonicalName().replace('$', '.');
                            String guessedName = nuClass.getCanonicalName().replace('$', '.') + "." + tr[i];
                            if (name.equals(guessedName)) {
                                nuClass = aClass;
                                break;
                            }
                        }
                    }
                }
            }
            String nuKey = tr[tr.length - 1];
            return new KeyValuePair(nuKey, String.valueOf(nuClass.getField(nuKey).get(nuObj)));
        } catch (Exception e) {
            if (obj != null) {
                // fallback to real system info
                return createSystemInfoPair(key, Build.class, null);
            }
            Log.w(TAG, e);
            return new KeyValuePair(key, "unknown");
        }
    }

    private static List<ByteString> getCached(Context context) {
        List<ByteString> res = new ArrayList<>();
        File dgDir = context.getDir("dg_cache", 0);
        for (String cache : dgDir.list()) {
            if (new File(dgDir, cache).isDirectory()) {
                res.add(ByteString.decodeHex(cache));
            }
        }
        return res;
    }

    private static void downloadAndPrepareDg(DGResponse response, File dgCacheDir, File dgDir, File dgFile) throws IOException {
        File dgCacheFile = new File(dgCacheDir, response.vmChecksum.hex() + ".apk");
        if (!dgFile.exists()) {
            if (response.content == null) {
                Log.d(TAG, "Downloading DG implementation from " + response.vmUrl + " to " + dgCacheFile);
                InputStream is = new URL(response.vmUrl).openStream();
                streamToFile(is, dgCacheFile);
            } else {
                Log.d(TAG, "Using provided response data for " + dgCacheFile);
                FileOutputStream fos = new FileOutputStream(dgCacheFile);
                fos.write(response.content.toByteArray());
                fos.close();
            }
            new File(dgDir, "opt").mkdirs();
            File libDir = new File(dgDir, "lib");
            libDir.mkdirs();
            ZipFile zipFile = new ZipFile(dgCacheFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    String targetName;
                    if (name.startsWith("lib/" + Build.CPU_ABI + "/")) {
                        targetName = name.substring(Build.CPU_ABI.length() + 5);
                    } else if (name.startsWith("lib/" + Build.CPU_ABI2 + "/")) {
                        targetName = name.substring(Build.CPU_ABI2.length() + 5);
                    } else {
                        continue;
                    }
                    streamToFile(zipFile.getInputStream(entry), new File(libDir, targetName));
                }
            }
            dgCacheFile.renameTo(dgFile);
        } else {
            Log.d(TAG, "Using cached file from " + dgFile);
        }
    }

    private static void streamToFile(InputStream is, File targetFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(targetFile);
        byte[] bytes = new byte[1024];
        while (true) {
            int n = is.read(bytes);
            if (n < 0) {
                break;
            }
            fos.write(bytes, 0, n);
        }
        is.close();
        fos.close();
    }

    private static String getArch() {
        try {
            return System.getProperty("os.arch");
        } catch (Exception ex) {
            return "";
        }
    }

    private static SignedDGResponse request(DGRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(DG_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-protobuf");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", "DroidGuard/" + Constants.GMS_VERSION_CODE);

        Log.d(TAG, "-- Request --\n" + request);
        OutputStream os = connection.getOutputStream();
        os.write(request.toByteArray());
        os.close();

        if (connection.getResponseCode() != 200) {
            byte[] bytes = null;
            String ex;
            try {
                bytes = readStreamToEnd(connection.getErrorStream());
                ex = new String(readStreamToEnd(new GZIPInputStream(new ByteArrayInputStream(bytes))));
            } catch (Exception e) {
                if (bytes != null) {
                    throw new IOException(getBytesAsString(bytes), e);
                }
                throw new IOException(connection.getResponseMessage(), e);
            }
            throw new IOException(ex);
        }

        InputStream is = connection.getInputStream();
        SignedDGResponse response = new Wire().parseFrom(new GZIPInputStream(is), SignedDGResponse.class);
        is.close();
        return response;
    }

    private static byte[] readStreamToEnd(final InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (is != null) {
            final byte[] buff = new byte[1024];
            int read;
            do {
                bos.write(buff, 0, (read = is.read(buff)) < 0 ? 0 : read);
            } while (read >= 0);
            is.close();
        }
        return bos.toByteArray();
    }

    private static String getBytesAsString(byte[] bytes) {
        if (bytes == null) return "null";
        try {
            CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
            CharBuffer r = d.decode(ByteBuffer.wrap(bytes));
            return r.toString();
        } catch (Exception e) {
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        }
    }

    public static final class Callback {

        private String packageName;
        private String androidIdLong;

        public Callback(String packageName, String androidIdLong) {
            this.packageName = packageName;
            this.androidIdLong = androidIdLong;
        }

        public final String a(final byte[] array) {
            String guasso = new String(DroidguassoHelper.guasso(array));
            Log.d(TAG, "a: " + Base64.encodeToString(array, Base64.NO_WRAP) + " -> " + guasso);
            return guasso;
        }

        public final String b() {
            Log.d(TAG, "b -> " + androidIdLong);
            return androidIdLong;
        }

        public final String c() {
            Log.d(TAG, "c -> " + packageName);
            return packageName;
        }

        public final void d(final Object o, final byte[] array) {
            Log.d(TAG, "d: " + o + ", " + Base64.encodeToString(array, Base64.NO_WRAP));
        }
    }
}
