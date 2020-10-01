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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedValueServiceTest {
    private EncryptedValueService service;

    @BeforeEach
    void setUp() {
        this.service = new EncryptedValueService("1234567890abcdef");
    }

    @Test
    void encryptAndDecryption() {
        final EncryptedValue encryptedValue = service.encrypt("s3cr3t");

        assertThat(encryptedValue.value()).isNotBlank().isNotEqualTo("s3cr3t");
        assertThat(encryptedValue.salt()).isNotBlank();

        assertThat(service.decrypt(encryptedValue)).isEqualTo("s3cr3t");
    }
}
