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

import com.google.common.hash.Hashing;
import org.graylog2.Configuration;

import javax.inject.Inject;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AccessTokenCipher {

    private final byte[] encryptionKey;

    @Inject
    public AccessTokenCipher(Configuration configuration) {
        // The password secret is only required to be at least 16 bytes long. Since the encryptSiv/decryptSiv methods
        // in AESTools require an encryption key that is at least 32 bytes long, we create a hash of the value.
        encryptionKey = Hashing.sha256().hashString(configuration.getPasswordSecret(), UTF_8).asBytes();
    }

    public String encrypt(String cleartext) {
        return AESTools.encryptSiv(cleartext, encryptionKey);
    }

    public String decrypt(String ciphertext) {
        return AESTools.decryptSiv(ciphertext, encryptionKey);
    }
}
