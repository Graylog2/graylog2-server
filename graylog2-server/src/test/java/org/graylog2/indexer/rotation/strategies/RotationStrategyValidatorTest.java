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
package org.graylog2.indexer.rotation.strategies;

import com.github.joschi.jadconfig.ValidationException;
import org.graylog2.configuration.validators.RotationStrategyValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class RotationStrategyValidatorTest {
    private final RotationStrategyValidator validator = new RotationStrategyValidator();
    private final String PARAM = "parameter-name";

    @Test
    void nullSet() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, null);
        });
    }

    @Test
    void emptySet() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, new ArrayList<>());
        });
    }

    @Test
    void duplicates() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, Arrays.asList("1", "1"));
        });
    }

    @Test
    void invalidStrategy() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, Arrays.asList("invalid-strategy"));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, Arrays.asList(
                    TimeBasedRotationStrategy.NAME,
                    "invalid-strategy"));
        });
    }

    @Test
    void validStrategy() throws ValidationException {
        validator.validate(PARAM, Arrays.asList(
                TimeBasedRotationStrategy.NAME
        ));
        validator.validate(PARAM, Arrays.asList(
                TimeBasedRotationStrategy.NAME,
                SizeBasedRotationStrategy.NAME,
                MessageCountRotationStrategy.NAME
        ));
    }
}
