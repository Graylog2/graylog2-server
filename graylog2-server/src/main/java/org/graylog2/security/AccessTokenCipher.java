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
