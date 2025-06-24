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
package org.graylog.security.shares;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateEntityRequestTest {
    record TestEntity(@NotBlank String title) {
    }

    @Test
    void validation() {
        final var validTitle = CreateEntityRequest.create(new TestEntity("test"), null);
        final var emptyTitle = CreateEntityRequest.create(new TestEntity(""), null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            final var validator = factory.getValidator();

            assertThat(validator.validate(validTitle)).isEmpty();

            assertThat(validator.validate(emptyTitle)).isNotEmpty().first().satisfies(error -> {
                assertThat(error.getPropertyPath().toString()).isEqualTo("entity.title");
                assertThat(error.getMessage()).isEqualTo("must not be blank");
            });
        }
    }
}
