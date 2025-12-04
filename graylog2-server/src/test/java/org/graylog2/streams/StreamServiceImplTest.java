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
package org.graylog2.streams;

import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityRegistrar;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class StreamServiceImplTest {
    protected static final String STREAM_ID = "5628f4503b0c5756a8eebc4d";

    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private OutputService outputService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory factory;
    @Mock
    private EntityRegistrar entityRegistrar;
    @Mock
    ClusterEventBus eventBus;

    private StreamService streamService;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) throws Exception {
        this.streamService = new StreamServiceImpl(mongoCollections, streamRuleService,
                outputService, indexSetService, factory, entityRegistrar, eventBus, Set.of(), new EntityScopeService(Set.of(new DefaultEntityScope(), new ImmutableSystemScope())));
    }

    @Test
    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    public void loadByIds() {
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a537197af2"))).hasSize(1);
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a5deadbeef"))).isEmpty();
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a537197af2", "565f02223b0c25a5deadbeef"))).hasSize(1);
    }

    @Test
    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    public void loadStreamTitles() {
        final var result = streamService.loadStreamTitles(Set.of("565f02223b0c25a537197af2", "559d14663b0cf26a15ee0f01"));

        assertThat(result).hasSize(2);
        assertThat(result.get("565f02223b0c25a537197af2")).isEqualTo("Logins");
        assertThat(result.get("559d14663b0cf26a15ee0f01")).isEqualTo("footitle");

        assertThat(streamService.loadStreamTitles(Set.of())).isEmpty();

        // Invalid ObjectIds throw an error
        assertThatThrownBy(() -> streamService.loadStreamTitles(Set.of("foo")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> streamService.loadStreamTitles(Collections.singleton(null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> streamService.loadStreamTitles(Collections.singleton("")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @MongoDBFixtures("someStreamsWithAlertConditions.json")
    public void streamTitleFromCache() {
        assertThat(streamService.streamTitleFromCache("565f02223b0c25a537197af2")).isEqualTo("Logins");
        assertThat(streamService.streamTitleFromCache("5628f4503b00deadbeef0002")).isNull();
    }

    @Test
    @MongoDBFixtures("someStreamsWithoutAlertConditions.json")
    public void addOutputs() throws NotFoundException {
        final ObjectId streamId = new ObjectId(STREAM_ID);
        final ObjectId output1Id = new ObjectId("5628f4503b00deadbeef0001");
        final ObjectId output2Id = new ObjectId("5628f4503b00deadbeef0002");

        final Output output1 = mock(Output.class);
        final Output output2 = mock(Output.class);

        when(output1.getId()).thenReturn(output1Id.toHexString());
        when(output2.getId()).thenReturn(output2Id.toHexString());
        when(outputService.loadByIds(Set.of(output1Id.toHexString(), output2Id.toHexString())))
                .thenReturn(Set.of(output1, output2));

        streamService.addOutputs(streamId, ImmutableSet.of(output1Id, output2Id));

        final Stream stream = streamService.load(streamId.toHexString());
        assertThat(stream.getOutputs())
                .anySatisfy(output -> assertThat(output.getId()).isEqualTo(output1Id.toHexString()))
                .anySatisfy(output -> assertThat(output.getId()).isEqualTo(output2Id.toHexString()));
    }

    @Test
    @MongoDBFixtures("someStreamsWithoutAlertConditions.json")
    public void testSaveStream_streamsChangedEventSent() throws ValidationException, NotFoundException {
        final Stream stream = streamService.load(new ObjectId(STREAM_ID).toHexString());
        streamService.save(stream);
        verify(eventBus, times(1)).post(StreamsChangedEvent.create(STREAM_ID));
    }

    @Test
    @MongoDBFixtures("userIlluminateStreams.json")
    public void testCountBySource() {
        Map<String, Long> count = streamService.countBySource();

        assertThat(count).isEqualTo(Map.of(
                "illuminate_streams", 2L,
                "user_streams", 1L
        ));
    }
}
