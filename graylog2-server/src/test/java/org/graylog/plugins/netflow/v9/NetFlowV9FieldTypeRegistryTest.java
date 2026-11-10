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
package org.graylog.plugins.netflow.v9;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NetFlowV9FieldTypeRegistryTest {
    @Test
    public void getReturnsExistingFieldType() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final NetFlowV9FieldType fieldType = typeRegistry.get(1);
        assertThat(fieldType.id()).isEqualTo(1);
        assertThat(fieldType.name()).isEqualTo("in_bytes");
        assertThat(fieldType.valueType()).isEqualTo(NetFlowV9FieldType.ValueType.UINT32);
    }

    @Test
    public void getReturnsNullForMissingFieldType() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final NetFlowV9FieldType fieldType = typeRegistry.get(123456);
        assertThat(fieldType).isNull();
    }

    @Test
    public void asMap() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final Map<Integer, NetFlowV9FieldType> map = typeRegistry.asMap();
        assertThat(map)
                .isNotEmpty()
                .containsEntry(1, NetFlowV9FieldType.create(1, NetFlowV9FieldType.ValueType.UINT32, "in_bytes"));
    }

    @Test
    public void skipFieldIsRegisteredWithSkipValueType() throws Exception {
        // Field 33000 is defined as [:skip] in the default netflow9.yml
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final NetFlowV9FieldType fieldType = typeRegistry.get(33000);
        assertThat(fieldType).as("skip field should be present in the registry").isNotNull();
        assertThat(fieldType.valueType()).isEqualTo(NetFlowV9FieldType.ValueType.SKIP);
    }

    @Test
    public void customSkipFieldIsRegisteredWithSkipValueType() throws Exception {
        final String yaml = "---\n235:\n- :skip\n";
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        final NetFlowV9FieldType fieldType = typeRegistry.get(235);
        assertThat(fieldType).as("custom skip field should be present in the registry").isNotNull();
        assertThat(fieldType.id()).isEqualTo(235);
        assertThat(fieldType.valueType()).isEqualTo(NetFlowV9FieldType.ValueType.SKIP);
    }

}