package org.microg.gms.droidguard;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.squareup.wire.Wire;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import okio.ByteString;

public class RemoteDroidGuardService extends Service {
    private static final String TAG = "GmsDroidGuardRemote";
    private static final String DG_CLASS_NAME = "com.google.ccc.abuse.droidguard.DroidGuard";
    private static final String DG_URL = "https://www.googleapis.com/androidantiabuse/v1/x/create?alt=PROTO&key=AIzaSyBofcZsgLSS7BOnBjZPEkk4rYwzOIz-lTI";
    private static Map<String, Class<?>> loadedClass = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return new IRemoteDroidGuard.Stub() {

            @Override
            public void guard(final IRemoteDroidGuardCallback callback, final String packageName, final String type, Bundle data) throws RemoteException {
                final Map map = new HashMap();
                for (String key : data.keySet()) {
                    String val = data.getString(key);
                    if (val != null) map.put(key, val);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onResult(RemoteDroidGuardService.this.guard(packageName, type, map));
                        } catch (Exception e) {
                            Log.w(TAG, e);
                            try {
                                callback.onError();
                            } catch (RemoteException e1) {
                                stopSelf();
                            }
                        }
                    }
                }).start();
            }
        };
    }

    private byte[] guard(String packageName, String type, Map map) throws Exception {
        SignedDGResponse signedResponse = request(new DGRequest.Builder()
                .usage(new DGUsage(type, packageName))
                .isGoogleCn(false)
                .someTrue(true)
                .currentVersion(3)
                .cached(getCached())
                .arch(getArch())
                .build());
        DGResponse response = new Wire().parseFrom(signedResponse.data.toByteArray(), DGResponse.class);
        String checksum = response.vmChecksum.hex();
        File dgCacheDir = getDir("dg_cache", 0);
        File dgDir = new File(dgCacheDir, checksum);
        dgDir.mkdirs();
        File dgFile = new File(dgDir, "the.apk");
        loadAndPrepareDg(response, dgCacheDir, dgDir, dgFile);

        Class<?> clazz;
        if (!loadedClass.containsKey(checksum)) {
            ClassLoader loader = new DexClassLoader(new File(dgDir, "the.apk").getAbsolutePath(),
                    new File(dgDir, "opt").getAbsolutePath(), new File(dgDir, "lib").getAbsolutePath(), getClassLoader());
            clazz = loader.loadClass(DG_CLASS_NAME);
            loadedClass.put(checksum, clazz);
        } else {
            clazz = loadedClass.get(checksum);
        }

        Object instance = clazz
                .getDeclaredConstructor(Context.class, String.class, byte[].class, Object.class)
                .newInstance(this, type, response.byteCode.toByteArray(), new Callback(packageName));
        clazz.getMethod("init").invoke(instance);
        byte[] bytes = (byte[]) clazz.getMethod("run", Map.class).invoke(instance, map);
        try {
            clazz.getMethod("close").invoke(instance);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return bytes;
    }

    private List<ByteString> getCached() {
        List<ByteString> res = new ArrayList<>();
        File dgDir = getDir("dg_cache", 0);
        for (String cache : dgDir.list()) {
            if (new File(dgDir, cache).isDirectory()) {
                res.add(ByteString.decodeHex(cache));
            }
        }
        return res;
    }

    private void loadAndPrepareDg(DGResponse response, File dgCacheDir, File dgDir, File dgFile) throws IOException {
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
        connection.setRequestProperty("Content-type", "application/x-protobuf");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("Accept-Encoding", "gzip");
        connection.setRequestProperty("User-Agent", "DroidGuard/" + 9452000); // TODO

        Log.d(TAG, "-- Request --\n" + request);
        OutputStream os = new GZIPOutputStream(connection.getOutputStream());
        os.write(request.toByteArray());
        os.close();

        if (connection.getResponseCode() != 200) {
            byte[] bytes = null;
            String ex = null;
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

    public final class Callback {

        private String packageName;

        public Callback(String packageName) {
            this.packageName = packageName;
        }

        public final String a(final byte[] array) {
            Log.d(TAG, "a: " + Base64.encodeToString(array, Base64.NO_WRAP));
            return "a";
        }

        public final String b() {
            Log.d(TAG, "b");
            return "b";
        }

        public final String c() {
            Log.d(TAG, "c");
            return packageName;
        }

        public final void d(final Object o, final byte[] array) {
            Log.d(TAG, "d: " + o + ", " + Base64.encodeToString(array, Base64.NO_WRAP));
        }
    }
}
