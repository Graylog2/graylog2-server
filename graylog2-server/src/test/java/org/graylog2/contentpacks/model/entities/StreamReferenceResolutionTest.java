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
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared behaviour for the view-side content pack resolvers that map a set of stream references to native stream ids
 * during installation. {@link WidgetEntity} and {@link SearchTypeEntity} implement the same contract, so their cases
 * are parameterized over a {@link StreamResolver} per subject. {@code QueryEntity} resolves stream references inside
 * filters rather than a stream set and is covered separately in {@code QueryEntityTest}.
 */
class StreamReferenceResolutionTest {

    /**
     * Maps a subject's content pack stream references to the resolved native stream ids, or throws a
     * {@link ContentPackException} when the entity map holds a non-{@link Stream} value for a reference.
     */
    @FunctionalInterface
    interface StreamResolver {
        Set<String> resolve(String subjectId, Set<String> streamRefs, Map<EntityDescriptor, Object> nativeEntities);
    }

    static List<Arguments> subjects() {
        return List.of(
                Arguments.of("widget", (StreamResolver) StreamReferenceResolutionTest::resolveViaWidget),
                Arguments.of("searchType", (StreamResolver) StreamReferenceResolutionTest::resolveViaSearchType));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subjects")
    void dropsUnresolvableStreamReferenceInsteadOfFailing(String subject, StreamResolver resolver) {
        assertThat(resolver.resolve("subject-id", Set.of("missing-stream-id"), Collections.emptyMap()))
                .isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subjects")
    void keepsResolvableStreamReferenceAndDropsUnresolvable(String subject, StreamResolver resolver) {
        assertThat(resolver.resolve("subject-id", Set.of("cp-stream-id", "missing-stream-id"),
                nativeStream("cp-stream-id", "native-stream-id")))
                .containsExactly("native-stream-id");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subjects")
    void failsOnWrongTypeStreamReference(String subject, StreamResolver resolver) {
        // A non-null value that is not a Stream indicates a corrupt entity map, which must still abort the install.
        final Map<EntityDescriptor, Object> nativeEntities =
                Map.of(EntityDescriptor.create("cp-stream-id", ModelTypes.STREAM_REF_V1), "not-a-stream");

        assertThatThrownBy(() -> resolver.resolve("subject-id", Set.of("cp-stream-id"), nativeEntities))
                .isInstanceOf(ContentPackException.class)
                .hasMessageContaining("cp-stream-id")
                .hasMessageContaining("subject-id");
    }

    private static Map<EntityDescriptor, Object> nativeStream(String contentPackId, String nativeId) {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn(nativeId);
        return Map.of(EntityDescriptor.create(contentPackId, ModelTypes.STREAM_REF_V1), stream);
    }

    private static Set<String> resolveViaWidget(String subjectId, Set<String> streamRefs,
                                                Map<EntityDescriptor, Object> nativeEntities) {
        final WidgetEntity widget = WidgetEntity.builder()
                .id(subjectId)
                .type(MessageListConfigDTO.NAME)
                .filters(Collections.emptyList())
                .timerange(KeywordRange.create("last 5 minutes", "Etc/UTC"))
                .query(ElasticsearchQueryString.of("*"))
                .streams(streamRefs)
                .config(MessageListConfigDTO.Builder.builder()
                        .fields(ImmutableSet.of())
                        .showMessageRow(false)
                        .build())
                .build();
        return widget.toNativeEntity(Collections.emptyMap(), nativeEntities).streams();
    }

    private static Set<String> resolveViaSearchType(String subjectId, Set<String> streamRefs,
                                                    Map<EntityDescriptor, Object> nativeEntities) {
        final MessageListEntity searchType = MessageListEntity.builder()
                .id(subjectId)
                .streams(ImmutableSet.copyOf(streamRefs))
                .build();
        return searchType.toNativeEntity(Collections.emptyMap(), nativeEntities).streams();
    }
}
