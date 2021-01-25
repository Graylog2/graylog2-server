/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.security;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    public void testEncryptDecryptWithKeyBeingLargerThan32Bytes() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "1234567890123456789012345678901234567", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "1234567890123456789012345678901234567", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }

    @Test
    public void testEncryptDecryptWith18BytesKey() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "123456789012345678", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "123456789012345678", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }

    @Test
    public void testEncryptDecryptWith16Characters17BytesKey() {
        byte[] iv = new byte[8];
        new SecureRandom().nextBytes(iv);
        final String encrypt = AESTools.encrypt("I am secret", "123456789012345\u00E4", Hex.encodeHexString(iv));
        final String decrypt = AESTools.decrypt(encrypt, "123456789012345\u00E4", Hex.encodeHexString(iv));
        Assert.assertEquals("I am secret", decrypt);
    }

    @Test
    public void sivEncryptAndDecrypt() throws Exception {
        final byte[] encryptionKey = DigestUtils.sha256("encryptionKey");
        final String secret = "secret";

        final String encrypt1 = AESTools.encryptSiv(secret, encryptionKey);
        final String encrypt2 = AESTools.encryptSiv(secret, encryptionKey);
        final String decrypt = AESTools.decryptSiv(encrypt1, encryptionKey);

        assertThat(decrypt).isEqualTo(secret);
        assertThat(encrypt1).isEqualTo(encrypt2);
    }

    @Test
    public void sivErrorConditions() {
        assertThatThrownBy(() -> AESTools.encryptSiv("foo", null))
                .hasMessageContaining("cannot be null")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.encryptSiv("foo", new byte[]{}))
                .hasMessageContaining("at least 32 bytes long")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.encryptSiv("foo", "bar".getBytes(UTF_8)))
                .hasMessageContaining("at least 32 bytes long")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.encryptSiv(null, DigestUtils.sha256("encryptionKey")))
                .hasMessageContaining("cannot be null")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> AESTools.decryptSiv("foo", null))
                .hasMessageContaining("cannot be null")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.decryptSiv("foo", new byte[]{}))
                .hasMessageContaining("at least 32 bytes long")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.decryptSiv("foo", "bar".getBytes(UTF_8)))
                .hasMessageContaining("at least 32 bytes long")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AESTools.decryptSiv(null, DigestUtils.sha256("encryptionKey")))
                .hasMessageContaining("cannot be null")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
