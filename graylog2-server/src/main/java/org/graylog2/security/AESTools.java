/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

import org.apache.shiro.codec.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AESTools {
    private static final Logger LOG = LoggerFactory.getLogger(AESTools.class);

    @Nullable
    public static String encrypt(String plainText, String encryptionKey, String salt) {
        try {
            @SuppressWarnings("CIPHER_INTEGRITY")
            Cipher cipher = Cipher.getInstance("AES/CBC/ISO10126Padding", "SunJCE");
            SecretKeySpec key = new SecretKeySpec(adjustToIdealKeyLength(encryptionKey), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(salt.getBytes("UTF-8")));
            return Hex.encodeToString(cipher.doFinal(plainText.getBytes("UTF-8")));
        } catch (Exception e) {
            LOG.error("Could not encrypt value.", e);
        }
        return null;
    }

    @Nullable
    public static String decrypt(String cipherText, String encryptionKey, String salt) {
        try {
            @SuppressWarnings("CIPHER_INTEGRITY")
            Cipher cipher = Cipher.getInstance("AES/CBC/ISO10126Padding", "SunJCE");
            SecretKeySpec key = new SecretKeySpec(adjustToIdealKeyLength(encryptionKey), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(salt.getBytes("UTF-8")));
            return new String(cipher.doFinal(Hex.decode(cipherText)), "UTF-8");
        } catch (Exception e) {
            LOG.error("Could not decrypt value.", e);
        }
        return null;
    }

    /**
     * Generates a new random salt
     *
     * @return the generated random salt as a string of hexadecimal digits.
     */
    public static String generateNewSalt() {
        final SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[8];
        random.nextBytes(saltBytes);
        return Hex.encodeToString(saltBytes);
    }

    private static int desiredKeyLength(String input) {
        final int length = input.length();

        if (length == 16 || length == 24 || length == 32) {
            return length;
        }

        if (length < 16) {
            return 16;
        }

        if (length > 32) {
            return 32;
        }

        return (length / 8 + 1) * 8;
    }

    private static byte[] cutToLength(String input, int length) {
        checkArgument(input.length() >= length, "Input string must be greater or of desired length");
        return (input.length() > length ? input.substring(0, length) : input).getBytes(UTF_8);
    }

    private static byte[] padToLength(String input, int length) {
        checkArgument(input.length() < length, "Input string must be smaller than desired length");
        final byte[] result = new byte[length];
        System.arraycopy(input.getBytes(UTF_8), 0, result, 0, input.length());

        return result;
    }

    private static byte[] cutOrPadToLength(String input, int length) {
        if (input.length() == length) {
            return input.getBytes(UTF_8);
        }

        return input.length() > length
                ? cutToLength(input, length)
                : padToLength(input, length);
    }

    private static byte[] adjustToIdealKeyLength(String input) {
        checkNotNull(input);
        final int desiredLength = desiredKeyLength(input);
        return cutOrPadToLength(input, desiredLength);
    }
}
