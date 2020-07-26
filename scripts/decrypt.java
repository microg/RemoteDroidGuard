/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.Key;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;

public class decrypt {
  public static void main(String[] args) throws Exception {
    IvParameterSpec iv = new IvParameterSpec(new byte[] { 1, 33, 13, 28, 87, 122, 25, 5, 4, 30, 22, 101, 5, 50, 12, 0 });
    byte[] key = new byte[16];
    byte[] bytes = "96a176a2e35d8ae4".getBytes();
    for (int n = 0; n < 16 && n < bytes.length; ++n) {
      key[n] = bytes[n];
    }
    SecretKeySpec k = new SecretKeySpec(key, "AES");
    Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
    c.init(2, k, iv);
    byte[] res = c.doFinal(readStreamToEnd(new FileInputStream(new File(args[0]))));
    FileOutputStream fos = new FileOutputStream(new File(args[1]));
    fos.write(res);
    fos.close();
  }

  public static byte[] readStreamToEnd(final InputStream is) throws IOException {
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
}

