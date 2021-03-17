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
package org.graylog2.shared.messageq.pulsar;

import com.github.joschi.jadconfig.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PulsarServiceUrlValidatorTest {

    @Test
    void validateValid() throws ValidationException {
        final PulsarServiceUrlValidator validator = new PulsarServiceUrlValidator();

        validator.validate("test", "pulsar://1.2.3.4:1111");
        validator.validate("test", "pulsar://host:1111");
        validator.validate("test", "pulsar+ssl://1.2.3.4:1111");
    }

    @Test
    void validateInvalid() {
        final PulsarServiceUrlValidator validator = new PulsarServiceUrlValidator();

        assertThrows(ValidationException.class, () -> validator.validate("test", "http://1.2.3.4:1111"));
        assertThrows(ValidationException.class, () -> validator.validate("test", "pulsar:/1.2.3.4:1111"));
        assertThrows(ValidationException.class, () -> validator.validate("test", "pulsar://999.2.3.4:1111"));
        assertThrows(ValidationException.class, () -> validator.validate("test", "pulsar://1.2.3.4"));
        assertThrows(ValidationException.class, () -> validator.validate("test", "pulsar://1.2.3.4:12345678"));
    }
}
