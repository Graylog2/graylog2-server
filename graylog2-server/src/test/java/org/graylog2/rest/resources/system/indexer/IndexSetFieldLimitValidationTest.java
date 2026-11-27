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
package org.graylog2.rest.resources.system.indexer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.createIndexSetConfig;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.toCreationRequest;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.toUpdateRequest;

class IndexSetFieldLimitValidationTest {
    private static final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = validatorFactory.getValidator();

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @CsvSource(value = {"null, true", "1000, true", "2000, true", "999, false", "2001, false"}, nullValues = "null")
    void acceptsFieldLimitsWithinRange(Integer fieldLimit, boolean valid) {
        final IndexSetConfig config = createIndexSetConfig().toBuilder().fieldLimit(fieldLimit).build();

        assertThat(hasFieldLimitViolation(validator.validate(toCreationRequest(config)))).isEqualTo(!valid);
        assertThat(hasFieldLimitViolation(validator.validate(toUpdateRequest(config)))).isEqualTo(!valid);
    }

    private static boolean hasFieldLimitViolation(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals("fieldLimit"));
    }
}
