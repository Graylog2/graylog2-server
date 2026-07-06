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
package org.graylog2.contentpacks.model.entities;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WidgetEntityTest {

    private WidgetEntity.Builder widgetBuilder(Set<String> streams) {
        return WidgetEntity.builder()
                .id("widget-id")
                .type(MessageListConfigDTO.NAME)
                .filters(Collections.emptyList())
                .timerange(KeywordRange.create("last 5 minutes", "Etc/UTC"))
                .query(ElasticsearchQueryString.of("*"))
                .streams(streams)
                .config(MessageListConfigDTO.Builder.builder()
                        .fields(ImmutableSet.of())
                        .showMessageRow(false)
                        .build());
    }

    @Test
    public void dropsUnresolvableStreamReferenceInsteadOfFailing() {
        final WidgetEntity widget = widgetBuilder(ImmutableSet.of("missing-stream-id")).build();

        final WidgetDTO nativeEntity = widget.toNativeEntity(Collections.emptyMap(), Collections.emptyMap());

        assertThat(nativeEntity.streams()).isEmpty();
    }

    @Test
    public void keepsResolvableStreamReference() {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("native-stream-id");
        final Map<EntityDescriptor, Object> nativeEntities =
                Map.of(EntityDescriptor.create("cp-stream-id", ModelTypes.STREAM_REF_V1), stream);

        final WidgetEntity widget = widgetBuilder(ImmutableSet.of("cp-stream-id")).build();

        final WidgetDTO nativeEntity = widget.toNativeEntity(Collections.emptyMap(), nativeEntities);

        assertThat(nativeEntity.streams()).containsExactly("native-stream-id");
    }
}
