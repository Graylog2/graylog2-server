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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetentionStrategyValidatorTest {

    private final RetentionStrategyValidator classUnderTest = new RetentionStrategyValidator();
    private final String PARAM = "parameter-name";

    @Test
    void failDisableAll() {
        assertThrows(ValidationException.class, () -> {
            classUnderTest.validate(PARAM, Set.of("none", "close", "delete"));
        });
    }

    @Test
    void invalidStrategy() {
        assertThrows(ValidationException.class, () -> {
            classUnderTest.validate(PARAM, Set.of("nonsense"));
        });
    }

    @Test
    void validStrategy() throws ValidationException {
        assertDoesNotThrow( () -> {
            classUnderTest.validate(PARAM, Set.of("none"));
            classUnderTest.validate(PARAM, Set.of("archive"));
            classUnderTest.validate(PARAM, Set.of("delete"));
            classUnderTest.validate(PARAM, Set.of("close"));
            classUnderTest.validate(PARAM, Set.of("none", "close"));
            classUnderTest.validate(PARAM, Set.of("none", "delete"));
            classUnderTest.validate(PARAM, Set.of("delete", "close"));
        });
    }
}
