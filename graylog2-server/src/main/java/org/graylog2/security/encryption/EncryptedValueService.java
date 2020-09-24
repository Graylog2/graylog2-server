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
                .build();
    }

    public String decrypt(EncryptedValue encryptedValue) {
        return AESTools.decrypt(encryptedValue.value(), encryptionKey, encryptedValue.salt());
    }
}
