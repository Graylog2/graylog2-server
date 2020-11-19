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
package org.graylog2.security.encryption;

import org.graylog2.security.AESTools;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@Singleton
public class EncryptedValueService {
    private final String encryptionKey;

    @Inject
    public EncryptedValueService(@Named("password_secret") String passwordSecret) {
        final String trimmedPasswordSecret = passwordSecret.trim();
        checkArgument(!isNullOrEmpty(trimmedPasswordSecret), "password secret cannot be null or empty");
        checkArgument(trimmedPasswordSecret.length() >= 16, "password secret must be at least 16 characters long");

        this.encryptionKey = trimmedPasswordSecret;
    }

    public EncryptedValue encrypt(String plainValue) {
        final String salt = AESTools.generateNewSalt();
        return EncryptedValue.builder()
                .value(AESTools.encrypt(plainValue, encryptionKey, salt))
                .salt(salt)
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();
    }

    public String decrypt(EncryptedValue encryptedValue) {
        return AESTools.decrypt(encryptedValue.value(), encryptionKey, encryptedValue.salt());
    }
}
