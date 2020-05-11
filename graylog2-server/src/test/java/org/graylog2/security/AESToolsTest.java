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

import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class AESToolsTest {

    @Test
    public void testEncryptDecrypt() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "1234567890123456", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "1234567890123456", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }

    @Test
    public void testEncryptWithKeyBeingLargerThan32() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "1234567890123456789012345678901234567", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "1234567890123456789012345678901234567", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }

    @Test
    public void testEncryptWithKeyBeingSmallerThan32() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "123456789012345678", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "123456789012345678", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }
}
