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
package org.graylog2.autovalue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test that validation annotations on AutoValue properties are correctly recognized by the validation framework.
 * This didn't work in the past, so we implemented the "@WithBeanGetter" annotation to work around the issue.
 * At some point auto-value started to add the annotations to the fields in the generated class, so this should now
 * work out of the box.
 */
public class AutoValueValidatorTest {
    private static final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = validatorFactory.getValidator();

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validates() {
        assertThat(validator.validate(TestValue.create("", 5)))
                .hasSize(1)
                .first()
                .satisfies(violation -> assertThat(violation.getMessage()).contains("not be blank"));

        assertThat(validator.validate(TestValue.create("hello", 0)))
                .hasSize(1)
                .first()
                .satisfies(violation -> assertThat(violation.getMessage()).contains("must be greater than or equal to 1"));
        assertThat(validator.validate(TestValue.create("hello", 100)))
                .hasSize(1)
                .first()
                .satisfies(violation -> assertThat(violation.getMessage()).contains("must be less than or equal to 10"));
    }

    @AutoValue
    static abstract class TestValue {
        @JsonProperty
        @NotBlank
        public abstract String text();

        @JsonProperty
        @Min(1)
        @Max(10)
        public abstract int number();

        public static TestValue create(String text, int number) {
            return new AutoValue_AutoValueValidatorTest_TestValue(text, number);
        }
    }
}
