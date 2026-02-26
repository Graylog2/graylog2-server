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
package org.graylog.collectors.config.processor;

import com.fasterxml.jackson.databind.node.TextNode;
import org.graylog.collectors.config.OtelAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceProcessorConfigTest {

    @Test
    void builderCreatesResourceProcessorName() {
        final var config = ResourceProcessorConfig.builder("filelog")
                .attributes(List.of(ResourceProcessorConfig.Attribute.upsert("a", "b")))
                .build();

        assertThat(config.name()).isEqualTo("resource/filelog");
    }

    @Test
    void collectorComponentAttributeUsesDefaultKeyAndUpsertAction() {
        final var attribute = ResourceProcessorConfig.collectorComponentAttribute("filelog");

        assertThat(attribute.key()).isEqualTo(OtelAttributes.COLLECTOR_RECEIVER_TYPE);
        assertThat(attribute.value().isTextual()).isTrue();
        assertThat(attribute.value().textValue()).isEqualTo("filelog");
        assertThat(attribute.action()).isEqualTo(ResourceProcessorConfig.Action.UPSERT);
    }

    @Test
    void upsertSupportsStringNumberAndBooleanValues() {
        final var stringAttribute = ResourceProcessorConfig.Attribute.upsert("string.key", "value");
        final var numberAttribute = ResourceProcessorConfig.Attribute.upsert("number.key", 42);
        final var booleanAttribute = ResourceProcessorConfig.Attribute.upsert("boolean.key", true);

        assertThat(stringAttribute.value().isTextual()).isTrue();
        assertThat(stringAttribute.value().textValue()).isEqualTo("value");

        assertThat(numberAttribute.value().isNumber()).isTrue();
        assertThat(numberAttribute.value().numberValue()).isEqualTo(42);

        assertThat(booleanAttribute.value().isBoolean()).isTrue();
        assertThat(booleanAttribute.value().booleanValue()).isTrue();
    }

    @Test
    void attributeRejectsBlankKey() {
        assertThatThrownBy(() -> new ResourceProcessorConfig.Attribute(
                " ",
                new TextNode("value"),
                ResourceProcessorConfig.Action.UPSERT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name can't be blank");
    }

    @Test
    void attributeRejectsNullValue() {
        assertThatThrownBy(() -> new ResourceProcessorConfig.Attribute(
                "valid.key",
                null,
                ResourceProcessorConfig.Action.UPSERT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value can't be null");
    }
}
