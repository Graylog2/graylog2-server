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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.exceptions.SkippableEntityException;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.NativeEntity;
import org.graylog2.contentpacks.model.entities.StreamReferenceEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.FavoriteFieldsService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamReferenceFacadeTest {
    private static final String TITLE = "Evaluation: Self Monitoring Logs";

    @Mock
    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private UserService userService;
    @Mock
    private FavoriteFieldsService favoriteFieldsService;

    private ObjectMapper objectMapper;
    private StreamReferenceFacade facade;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        facade = new StreamReferenceFacade(objectMapper, streamService, streamRuleService, indexSetService, userService, favoriteFieldsService);
    }

    @Test
    void findExistingSkipsWhenReferencedStreamIsMissing() {
        when(streamService.loadAllByTitle(TITLE)).thenReturn(List.of());

        assertThatThrownBy(() -> facade.findExisting(streamRefEntity(TITLE), Map.of()))
                .isInstanceOf(SkippableEntityException.class)
                .hasMessageContaining(TITLE);
    }

    @Test
    void findExistingHardFailsWhenReferencedStreamTitleIsAmbiguous() {
        // A duplicate stream title is a genuine, admin-fixable conflict, so it must still abort the install.
        when(streamService.loadAllByTitle(TITLE)).thenReturn(List.of(mock(Stream.class), mock(Stream.class)));

        assertThatThrownBy(() -> facade.findExisting(streamRefEntity(TITLE), Map.of()))
                .isInstanceOf(ContentPackException.class)
                .isNotInstanceOf(SkippableEntityException.class)
                .hasMessageContaining(TITLE);
    }

    @Test
    void findExistingResolvesWhenExactlyOneStreamMatches() {
        final Stream stream = mock(Stream.class);
        when(stream.getId()).thenReturn("native-stream-id");
        when(stream.getTitle()).thenReturn(TITLE);
        when(streamService.loadAllByTitle(TITLE)).thenReturn(List.of(stream));

        final Optional<NativeEntity<Stream>> existing = facade.findExisting(streamRefEntity(TITLE), Map.of());

        assertThat(existing).isPresent();
        assertThat(existing.get().entity()).isSameAs(stream);
    }

    @Test
    void createNativeEntitySkipsBecauseAStreamReferenceCannotBeCreated() {
        assertThatThrownBy(() -> facade.createNativeEntity(streamRefEntity(TITLE), Map.of(), Map.of(), "user"))
                .isInstanceOf(SkippableEntityException.class)
                .hasMessageContaining(TITLE);
    }

    private EntityV1 streamRefEntity(String title) {
        final StreamReferenceEntity streamEntity = StreamReferenceEntity.create(ValueReference.of(title));
        final JsonNode data = objectMapper.convertValue(streamEntity, JsonNode.class);
        return EntityV1.builder()
                .id(ModelId.of("stream-ref-cp-id"))
                .type(ModelTypes.STREAM_REF_V1)
                .data(data)
                .build();
    }
}
