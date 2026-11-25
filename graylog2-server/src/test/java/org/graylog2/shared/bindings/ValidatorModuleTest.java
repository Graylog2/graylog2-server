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
package org.graylog2.shared.bindings;

import com.google.inject.Guice;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Null;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests constraint messages that use Jakarta Expression Language (EL) templates.
 * Hibernate Validator 9.1.6+ uses EL 6 classes for interpolation, so these tests fail with a
 * {@link NoClassDefFoundError} when an incompatible EL version (e.g. EL 5 from Jersey) is on the classpath.
 */
class ValidatorModuleTest {
    private static Validator validator;

    // Must be public, so the EL resolver can access the record accessor methods
    public record Item(String name) {}

    static class Bean {
        @DecimalMax("10")
        BigDecimal max = BigDecimal.TEN;

        @DecimalMin(value = "1", inclusive = false)
        BigDecimal min = BigDecimal.TEN;

        @Null(message = "item ${validatedValue.name} is not allowed")
        Item item;
    }

    @BeforeAll
    static void setUp() {
        validator = Guice.createInjector(new ValidatorModule()).getInstance(Validator.class);
    }

    @Test
    void interpolatesDecimalMaxMessage() {
        final Bean bean = new Bean();
        bean.max = new BigDecimal("11");

        assertThat(validator.validate(bean))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("must be less than or equal to 10");
    }

    @Test
    void interpolatesDecimalMinMessage() {
        final Bean bean = new Bean();
        bean.min = BigDecimal.ONE;

        assertThat(validator.validate(bean))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("must be greater than 1");
    }

    @Test
    void interpolatesRecordComponentInMessage() {
        final Bean bean = new Bean();
        bean.item = new Item("foo");

        assertThat(validator.validate(bean))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("item foo is not allowed");
    }
}
