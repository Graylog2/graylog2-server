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
import org.graylog.plugins.views.search.SearchType;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchTypeEntityTest {

    @Test
    public void dropsUnresolvableStreamReferenceInsteadOfFailing() {
        final MessageListEntity searchType = MessageListEntity.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("missing-stream-id"))
                .build();

        final SearchType nativeEntity = searchType.toNativeEntity(Collections.emptyMap(), Collections.emptyMap());

        assertThat(nativeEntity.streams()).isEmpty();
    }

    @Test
    public void keepsResolvableStreamReference() {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("native-stream-id");
        final Map<EntityDescriptor, Object> nativeEntities =
                Map.of(EntityDescriptor.create("cp-stream-id", ModelTypes.STREAM_REF_V1), stream);

        final MessageListEntity searchType = MessageListEntity.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("cp-stream-id"))
                .build();

        final SearchType nativeEntity = searchType.toNativeEntity(Collections.emptyMap(), nativeEntities);

        assertThat(nativeEntity.streams()).containsExactly("native-stream-id");
    }

    @Test
    public void dropsOnlyUnresolvableStreamReferences() {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("native-stream-id");
        final Map<EntityDescriptor, Object> nativeEntities =
                Map.of(EntityDescriptor.create("cp-stream-id", ModelTypes.STREAM_REF_V1), stream);

        final MessageListEntity searchType = MessageListEntity.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("cp-stream-id", "missing-stream-id"))
                .build();

        final SearchType nativeEntity = searchType.toNativeEntity(Collections.emptyMap(), nativeEntities);

        assertThat(nativeEntity.streams()).containsExactly("native-stream-id");
    }

    @Test
    public void failsOnWrongTypeStreamReference() {
        // A non-null value that is not a Stream indicates a corrupt entity map, which must still abort the install.
        final Map<EntityDescriptor, Object> nativeEntities =
                Map.of(EntityDescriptor.create("cp-stream-id", ModelTypes.STREAM_REF_V1), "not-a-stream");

        final MessageListEntity searchType = MessageListEntity.builder()
                .id("search-type-id")
                .streams(ImmutableSet.of("cp-stream-id"))
                .build();

        assertThatThrownBy(() -> searchType.toNativeEntity(Collections.emptyMap(), nativeEntities))
                .isInstanceOf(ContentPackException.class)
                .hasMessageContaining("cp-stream-id")
                .hasMessageContaining("search-type-id");
    }
}
