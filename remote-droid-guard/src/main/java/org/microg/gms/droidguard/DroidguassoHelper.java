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

import com.google.ccc.abuse.droidguard.droidguasso.Droidguasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okio.ByteString;

public class DroidguassoHelper {
    private static final String TAG = "GmsDroidguassoHelper";

    private char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static byte[] guasso(byte[] digest) {
        List<String> substrs = new ArrayList<>();
        addFilesInPath("/vendor/lib/egl", substrs);
        addFilesInPath("/system/lib/egl", substrs);
        Collections.sort(substrs);
        substrs.add(initialBytesDigest(new File("/system/lib/egl/egl.cfg")));
        String eglInfo = hexDigest(substrs.toString().getBytes());


        float[] floats = new float[]{0.35502917f, 0.47196686f, 0.24689609f, 0.66850024f, 0.7746259f, 0.5967446f, 0.06270856f, 0.19201201f, 0.35090452f, 0.5573558f, 0.470259f, 0.9866341f};
        if (digest != null && digest.length >= 48) {
            ByteBuffer order = ByteBuffer.wrap(digest).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 12; i++) {
                floats[i] = ((float) Math.abs(order.getInt())) / 2.14748365E9f;
            }
        }

        Droidguasso dg = new Droidguasso();
        dg.render(floats);

        return ("5=" + eglInfo + "\n7=" + dg.getGpu() + "\n8=" + dg.getHash1() + "\n9=" + dg.getHash2() + "\n").getBytes();
    }

    private static String initialBytesDigest(File file) {
        try {
            FileInputStream is = new FileInputStream(file);
            byte[] bytes = new byte[(int) Math.min(1024, file.length())];
            is.read(bytes);
            is.close();
            return hexDigest(bytes);
        } catch (Exception e) {
            return "";
        }
    }

    private static String hexDigest(byte[] bytes) {
        try {
            return ByteString.of(MessageDigest.getInstance("SHA-1").digest(bytes)).hex();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static void addFilesInPath(String path, List list) {
        final File parent = new File(path);
        if (parent.isDirectory()) {
            final File[] listFiles = parent.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".so");
                }
            });
            for (File file : listFiles) {
                list.add(file.getName() + "/" + file.length() + "/" + initialBytesDigest(file));
            }
        }
    }
}
