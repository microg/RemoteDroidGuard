/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard;

import com.google.ccc.abuse.droidguard.droidguasso.Droidguasso;
import com.google.ccc.abuse.droidguard.droidguasso.DroidguassoException;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okio.ByteString;

/**
 * DroidGuasso helper class. It hashes some files up to a size of 1024 bytes and feeds them with a digest into DroidGuasso
 */
public class DroidguassoHelper {
    private static final String TAG = "GmsDroidguassoHelper";

    private char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Makes a call to DroidGuasso.
     *
     * @param digest A digest array to feed into DroidGuasso
     * @return DroidGuasso output data to further process
     */
    public static byte[] guasso(byte[] digest) throws DroidguassoException {
        List<String> substrs = new ArrayList<>();
        addFilesInPath("/vendor/lib/egl", substrs); //Hashing all EGL libs in /vendor
        addFilesInPath("/system/lib/egl", substrs); //Hashing all EGL libs in /system
        Collections.sort(substrs);
        substrs.add(initialBytesDigest(new File("/system/lib/egl/egl.cfg"))); //Hashing the EGL config
        String eglInfo = hexDigest(substrs.toString().getBytes()); //SHA-1 hashing the toString() of an object?


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

    /**
     * SHA-1 hashes file contents. Max size is 1024 bytes.
     *
     * @param file A file object which contents to hash
     * @return Returns the hash
     */
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

    /**
     * SHA-1 hashes an byte array
     *
     * @param bytes Bytes to hash
     * @return Returns the hash
     */
    private static String hexDigest(byte[] bytes) {
        try {
            return ByteString.of(MessageDigest.getInstance("SHA-1").digest(bytes)).hex();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    /**
     * Finds ".so" files in a directory and adds their SHA-1 hashed content to a given {@link List} object.
     * Representation in the list as follows:
     * {@literal <filename>/<file_length>/<contents_hash>}
     *
     * @param path the parent directory to search for ".so" files
     * @param list the list to add the
     */
    private static void addFilesInPath(String path, List<String> list) {
        final File parent = new File(path);
        if (parent.isDirectory()) {
            final File[] listFiles = parent.listFiles((dir, filename) -> filename.endsWith(".so"));

            for (File file : listFiles)
                list.add(file.getName() + "/" + file.length() + "/" + initialBytesDigest(file));
        }
    }
}
