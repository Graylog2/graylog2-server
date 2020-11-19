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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.value.AutoValue;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class EncryptedValueTest {
    private TestService dbService;
    private ObjectMapper objectMapper;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        encryptedValueService = new EncryptedValueService("1234567890abcdef");
        this.objectMapper = new ObjectMapperProvider(
                ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes()
        ).get();

        this.dbService = new TestService(mongodb.mongoConnection(), new MongoJackObjectMapperProvider(objectMapper));
    }

    @Test
    void test() throws Exception {
        final EncryptedValue value = EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();

        final String jsonString = objectMapper.writeValueAsString(TestDTO.create("abc123", value));
        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("password_value").path("is_set").isBoolean()).isTrue();
        assertThat(node.path("password_value").path("is_set").asBoolean()).isTrue();
    }

    @Test
    void testUnset() throws Exception {
        final EncryptedValue value = EncryptedValue.createUnset();

        final String jsonString = objectMapper.writeValueAsString(TestDTO.create("abc123", value));
        final JsonNode node = objectMapper.readValue(jsonString, JsonNode.class);

        assertThat(node.path("password_value").path("is_set").isBoolean()).isTrue();
        assertThat(node.path("password_value").path("is_set").asBoolean()).isFalse();
    }

    @Test
    void testDeserialize() throws Exception {
        final TestDTO dto = objectMapper.readValue("{\"id\":\"abc123\",\"password_value\":{\"set_value\":\"new-password\"}}", TestDTO.class);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue().value()).isNotBlank();
        assertThat(dto.passwordValue().salt()).isNotBlank();
        assertThat(dto.passwordValue().isKeepValue()).isFalse();
        assertThat(dto.passwordValue().isDeleteValue()).isFalse();
        assertThat(encryptedValueService.decrypt(dto.passwordValue())).isEqualTo("new-password");
    }

    @Test
    void testDeserializeString() throws Exception {
        final TestDTO dto = objectMapper.readValue("{\"id\":\"abc123\",\"password_value\":\"new-password\"}", TestDTO.class);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue().value()).isNotBlank();
        assertThat(dto.passwordValue().salt()).isNotBlank();
        assertThat(dto.passwordValue().isKeepValue()).isFalse();
        assertThat(dto.passwordValue().isDeleteValue()).isFalse();
        assertThat(encryptedValueService.decrypt(dto.passwordValue())).isEqualTo("new-password");
    }

    @Test
    void testDeserializeWithKeepValue() throws Exception {
        final TestDTO dto = objectMapper.readValue("{\"id\":\"abc123\",\"password_value\":{\"keep_value\":true}}", TestDTO.class);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue().value()).isBlank();
        assertThat(dto.passwordValue().salt()).isBlank();
        assertThat(dto.passwordValue().isKeepValue()).isTrue();
        assertThat(dto.passwordValue().isDeleteValue()).isFalse();
    }

    @Test
    void testDeserializeWithDeleteValue() throws Exception {
        final TestDTO dto = objectMapper.readValue("{\"id\":\"abc123\",\"password_value\":{\"delete_value\":true}}", TestDTO.class);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue().value()).isBlank();
        assertThat(dto.passwordValue().salt()).isBlank();
        assertThat(dto.passwordValue().isKeepValue()).isFalse();
        assertThat(dto.passwordValue().isDeleteValue()).isTrue();
    }

    @Test
    void testDeserializeNullValue() throws Exception {
        final TestDTO dto = objectMapper.readValue("{\"id\":\"abc123\",\"password_value\":null}", TestDTO.class);

        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue().value()).isEmpty();
        assertThat(dto.passwordValue().salt()).isEmpty();
        assertThat(dto.passwordValue().isKeepValue()).isFalse();
        assertThat(dto.passwordValue().isDeleteValue()).isFalse();
    }

    @Test
    void testWithDatabase() {
        final EncryptedValue value = EncryptedValue.builder()
                .value("2d043f9a7d5a5a7537d3e93c93c5dc40")
                .salt("c93c0263bfc3713d")
                .isKeepValue(false)
                .isDeleteValue(false)
                .build();

        final String savedId = dbService.save(TestDTO.create(value)).id();
        final TestDTO dto = dbService.get(savedId).orElse(null);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue()).isEqualTo(value);
        assertThat(dto.passwordValue().isSet()).isTrue();
        assertThat(dto.passwordValue().value()).isEqualTo("2d043f9a7d5a5a7537d3e93c93c5dc40");
        assertThat(dto.passwordValue().salt()).isEqualTo("c93c0263bfc3713d");
    }

    @Test
    void testUnsetWithDatabase() {
        final String savedId = dbService.save(TestDTO.create(EncryptedValue.createUnset())).id();
        final TestDTO dto = dbService.get(savedId).orElse(null);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isNotBlank();
        assertThat(dto.passwordValue()).isEqualTo(EncryptedValue.createUnset());
        assertThat(dto.passwordValue().isSet()).isFalse();
        assertThat(dto.passwordValue().value()).isEmpty();
        assertThat(dto.passwordValue().salt()).isEmpty();
    }

    static class TestService extends PaginatedDbService<TestDTO> {
        @Override
        public Optional<TestDTO> get(String id) {
            return super.get(id);
        }

        protected TestService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapperProvider) {
            super(mongoConnection, mapperProvider, TestDTO.class, "test_collection");
        }
    }

    @AutoValue
    static abstract class TestDTO {
        @Id
        @ObjectId
        @Nullable
        @JsonProperty
        public abstract String id();

        @JsonProperty
        public abstract EncryptedValue passwordValue();

        public static TestDTO create(@JsonProperty EncryptedValue passwordValue) {
            return create(null, passwordValue);
        }

        @JsonCreator
        public static TestDTO create(@JsonProperty("id") String id, @JsonProperty("password_value") EncryptedValue passwordValue) {
            return new AutoValue_EncryptedValueTest_TestDTO(id, passwordValue);
        }
    }
}
