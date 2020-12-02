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
