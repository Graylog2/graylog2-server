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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import org.graylog.testing.ObjectMapperExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.utilities.AssertJsonPath.assertJsonPath;

@ExtendWith(ObjectMapperExtension.class)
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

    @Test
    void serialization(ObjectMapper mapper) throws Exception {
        final var req1 = CreateEntityRequest.create(new TestEntity("test"), null);
        final var req2 = CreateEntityRequest.create(new TestEntity("test2"), EntityShareRequest.EMPTY);

        assertJsonPath(mapper.writeValueAsString(req1), jsonPathAssert -> {
            jsonPathAssert.jsonPathAsString("$.entity.title").isEqualTo("test");
            jsonPathAssert.jsonPath("$.share_request").isNull();
        });

        assertJsonPath(mapper.writeValueAsString(req2), jsonPathAssert -> {
            jsonPathAssert.jsonPathAsString("$.entity.title").isEqualTo("test2");
            jsonPathAssert.jsonPathAsListOf("$.share_request.selected_collections", String.class).isEmpty();
            jsonPathAssert.jsonPathAsMap("$.share_request.selected_grantee_capabilities").isEmpty();
        });
    }

    @Test
    void deserialization(ObjectMapper mapper) throws Exception {
        final var json1 = mapper.writeValueAsString(CreateEntityRequest.create(new TestEntity("test"), null));
        final var json2 = mapper.writeValueAsString(CreateEntityRequest.create(new TestEntity("test2"), EntityShareRequest.EMPTY));
        final var req1 = mapper.readValue(json1, new TypeReference<CreateEntityRequest<TestEntity>>() {});
        final var req2 = mapper.readValue(json2, new TypeReference<CreateEntityRequest<TestEntity>>() {});

        assertThat(req1.entity().title()).isEqualTo("test");
        assertThat(req1.shareRequest()).isEmpty();

        assertThat(req2.entity().title()).isEqualTo("test2");
        assertThat(req2.shareRequest()).get().satisfies(req -> {
            assertThat(req.selectedCollections()).isPresent();
            assertThat(req.selectedGranteeCapabilities()).isPresent();
        });
    }
}
