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
package org.graylog2.contentpacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.facades.StreamFacade;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentPackEntityResolverTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private StreamService streamService;
    private ContentPackEntityResolver contentPackEntityResolver;

    @BeforeEach
    void setUp() {
        this.streamService = mock(StreamService.class);
        final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades = ImmutableMap.of(
                ModelTypes.STREAM_V1, new StreamFacade(objectMapper, streamService, mock(StreamRuleService.class), mock(IndexSetService.class), mock(UserService.class))
        );
        this.contentPackEntityResolver = new ContentPackEntityResolver(entityFacades, streamService);
    }

    @Test
    public void resolveEntitiesWithEmptyInput() {
        final Set<EntityDescriptor> resolvedEntities = contentPackEntityResolver.resolveEntities(Collections.emptySet());
        assertThat(resolvedEntities).isEmpty();
    }

    @Test
    public void resolveEntitiesWithNoDependencies() throws NotFoundException {
        final StreamMock streamMock = new StreamMock(ImmutableMap.of(
                "_id", "stream-1234",
                StreamImpl.FIELD_TITLE, "Stream Title"
        ));

        when(streamService.load("stream-1234")).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1)
        );

        final Set<EntityDescriptor> resolvedEntities = contentPackEntityResolver.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(EntityDescriptor.create("stream-1234", ModelTypes.STREAM_V1));
    }


    @Test
    public void resolveEntitiesWithTransitiveDependencies() throws NotFoundException {
        final ObjectId streamId = ObjectId.get();
        final ObjectId outputId = ObjectId.get();
        final StreamMock streamMock = new StreamMock(streamId,
                ImmutableMap.of(
                        StreamImpl.FIELD_TITLE, "Stream Title"),
                List.of(),
                Set.of(OutputImpl.create(
                        outputId.toHexString(),
                        "Output Title",
                        "org.example.outputs.SomeOutput",
                        "admin",
                        Collections.emptyMap(),
                        new Date(0L),
                        null
                )), null);

        when(streamService.load(streamId.toHexString())).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create(streamId.toHexString(), ModelTypes.STREAM_V1)
        );

        final Set<EntityDescriptor> resolvedEntities = contentPackEntityResolver.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(
                EntityDescriptor.create(streamId.toHexString(), ModelTypes.STREAM_V1),
                EntityDescriptor.create(outputId.toHexString(), ModelTypes.OUTPUT_V1)
        );
    }
}
