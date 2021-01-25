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
package org.graylog.events.event;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class EventOriginContextTest {
    @Test
    public void elasticsearchMessage() {
        assertThat(EventOriginContext.elasticsearchMessage("graylog_0", "b5e53442-12bb-4374-90ed-c325c0d979ce"))
                .isEqualTo("urn:graylog:message:es:graylog_0:b5e53442-12bb-4374-90ed-c325c0d979ce");

        assertThatCode(() -> EventOriginContext.elasticsearchMessage("", "b5e53442-12bb-4374-90ed-c325c0d979ce"))
                .hasMessageContaining("indexName")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventOriginContext.elasticsearchMessage(null, "b5e53442-12bb-4374-90ed-c325c0d979ce"))
                .hasMessageContaining("indexName")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatCode(() -> EventOriginContext.elasticsearchMessage("graylog_0", ""))
                .hasMessageContaining("messageId")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventOriginContext.elasticsearchMessage("graylog_0", null))
                .hasMessageContaining("messageId")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void elasticsearchEvent() {
        assertThat(EventOriginContext.elasticsearchEvent("gl-events_0", "01DF13GB094MT6390TYQB2Q73Q"))
                .isEqualTo("urn:graylog:event:es:gl-events_0:01DF13GB094MT6390TYQB2Q73Q");

        assertThatCode(() -> EventOriginContext.elasticsearchEvent("", "01DF13GB094MT6390TYQB2Q73Q"))
                .hasMessageContaining("indexName")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventOriginContext.elasticsearchEvent(null, "01DF13GB094MT6390TYQB2Q73Q"))
                .hasMessageContaining("indexName")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatCode(() -> EventOriginContext.elasticsearchEvent("gl-events_0", ""))
                .hasMessageContaining("eventId")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> EventOriginContext.elasticsearchEvent("gl-events_0", null))
                .hasMessageContaining("eventId")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void parseWrongESContext() {
        assertThat(EventOriginContext.parseESContext("urn:moo")).isEmpty();
    }

    @Test
    public void parseShortESContext() {
        assertThat(EventOriginContext.parseESContext("urn:graylog:message:es:ind")).isEmpty();
    }

    @Test
    public void parseMessageESContext() {
        assertThat(EventOriginContext.parseESContext("urn:graylog:message:es:index-42:01DF13GB094MT6390TYQB2Q73Q"))
                .isPresent()
                .get()
                .satisfies(context -> {
                    assertThat(context.indexName()).isEqualTo("index-42");
                    assertThat(context.messageId()).isEqualTo("01DF13GB094MT6390TYQB2Q73Q");
                });
    }

    @Test
    public void parseEventESContext() {
        assertThat(EventOriginContext.parseESContext("urn:graylog:event:es:index-42:01DF13GB094MT6390TYQB2Q73Q"))
                .isPresent()
                .get()
                .satisfies(context -> {
                    assertThat(context.indexName()).isEqualTo("index-42");
                    assertThat(context.messageId()).isEqualTo("01DF13GB094MT6390TYQB2Q73Q");
                });
    }
}