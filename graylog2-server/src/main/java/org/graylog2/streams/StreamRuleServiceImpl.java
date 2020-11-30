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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.graylog2.streams.events.StreamsChangedEvent;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamRuleServiceImpl extends PersistedServiceImpl implements StreamRuleService {
    private final ClusterEventBus clusterEventBus;

    @Inject
    public StreamRuleServiceImpl(MongoConnection mongoConnection,
                                 ClusterEventBus clusterEventBus) {
        super(mongoConnection);
        collection(StreamRuleImpl.class).createIndex(StreamRuleImpl.FIELD_STREAM_ID);
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public StreamRule load(String id) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(StreamRuleImpl.class, new ObjectId(id));

        if (o == null) {
            throw new NotFoundException("Couldn't find stream rule with ID" + id);
        }

        return new StreamRuleImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public List<StreamRule> loadForStream(Stream stream) {
        return loadForStreamId(stream.getId());
    }

    @Override
    public StreamRule create(Map<String, Object> data) {
        return new StreamRuleImpl(data);
    }

    @Override
    public StreamRule create(@Nullable String streamId, CreateStreamRuleRequest cr) {
        Map<String, Object> streamRuleData = Maps.newHashMap();
        streamRuleData.put(StreamRuleImpl.FIELD_TYPE, cr.type());
        streamRuleData.put(StreamRuleImpl.FIELD_VALUE, cr.value());
        streamRuleData.put(StreamRuleImpl.FIELD_FIELD, cr.field());
        streamRuleData.put(StreamRuleImpl.FIELD_INVERTED, cr.inverted());
        streamRuleData.put(StreamRuleImpl.FIELD_DESCRIPTION, cr.description());

        if (streamId != null) {
            streamRuleData.put(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamId));
        }

        return new StreamRuleImpl(streamRuleData);
    }

    @Override
    public StreamRule copy(@Nullable String streamId, StreamRule streamRule) {
        Map<String, Object> streamRuleData = Maps.newHashMap();
        streamRuleData.put(StreamRuleImpl.FIELD_TYPE, streamRule.getType().toInteger());
        streamRuleData.put(StreamRuleImpl.FIELD_VALUE, streamRule.getValue());
        streamRuleData.put(StreamRuleImpl.FIELD_FIELD, streamRule.getField());
        streamRuleData.put(StreamRuleImpl.FIELD_INVERTED, streamRule.getInverted());
        streamRuleData.put(StreamRuleImpl.FIELD_DESCRIPTION, streamRule.getDescription());

        if (streamId != null) {
            streamRuleData.put(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamId));
        }

        return new StreamRuleImpl(streamRuleData);
    }

    @Override
    public String save(StreamRule streamRule) throws ValidationException {
        final String streamId = streamRule.getStreamId();
        final String savedStreamRuleId = super.save(streamRule);
        clusterEventBus.post(StreamsChangedEvent.create(streamId));

        return savedStreamRuleId;
    }

    @Override
    public Set<String> save(Collection<StreamRule> streamRules) throws ValidationException {
        final ImmutableSet.Builder<String> streamIds = ImmutableSet.builder();
        final ImmutableSet.Builder<String> streamRuleIds = ImmutableSet.builder();
        for (StreamRule streamRule : streamRules) {
            final String streamId = streamRule.getStreamId();
            final String savedStreamRuleId = super.save(streamRule);
            streamIds.add(streamId);
            streamRuleIds.add(savedStreamRuleId);
        }
        clusterEventBus.post(StreamsChangedEvent.create(streamIds.build()));

        return streamRuleIds.build();
    }

    @Override
    public int destroy(StreamRule streamRule) {
        final String streamId = streamRule.getStreamId();
        final int deletedStreamRules = super.destroy(streamRule);
        clusterEventBus.post(StreamsChangedEvent.create(streamId));

        return deletedStreamRules;
    }

    @Override
    public List<StreamRule> loadForStreamId(String streamId) {
        ObjectId id = new ObjectId(streamId);
        final List<StreamRule> streamRules = new ArrayList<>();
        final List<DBObject> respStreamRules = query(StreamRuleImpl.class,
                new BasicDBObject(StreamRuleImpl.FIELD_STREAM_ID, id)
        );

        for (DBObject streamRule : respStreamRules) {
            streamRules.add(toStreamRule(streamRule));
        }

        return streamRules;
    }

    @Override
    public Map<String, List<StreamRule>> loadForStreamIds(Collection<String> streamIds) {
        final List<ObjectId> objectIds = streamIds.stream()
            .map(ObjectId::new)
            .collect(Collectors.toList());

        final List<DBObject> respStreamRules = query(StreamRuleImpl.class,
            new BasicDBObject(StreamRuleImpl.FIELD_STREAM_ID, new BasicDBObject("$in", objectIds))
        );

        return respStreamRules.stream()
            .map(this::toStreamRule)
            .collect(Collectors.groupingBy(StreamRule::getStreamId));
    }

    @Override
    public long totalStreamRuleCount() {
        return totalCount(StreamRuleImpl.class);
    }

    @Override
    public long streamRuleCount(String streamId) {
        return streamRuleCount(new ObjectId(streamId));
    }

    private long streamRuleCount(ObjectId streamId) {
        return count(StreamRuleImpl.class, new BasicDBObject(StreamRuleImpl.FIELD_STREAM_ID, streamId));
    }

    @Override
    public Map<String, Long> streamRuleCountByStream() {
        final ImmutableMap.Builder<String, Long> streamRules = ImmutableMap.builder();
        try(DBCursor streamIds = collection(StreamImpl.class).find(new BasicDBObject(), new BasicDBObject("_id", 1))) {
            for (DBObject keys : streamIds) {
                final ObjectId streamId = (ObjectId) keys.get("_id");
                streamRules.put(streamId.toHexString(), streamRuleCount(streamId));
            }
        }

        return streamRules.build();
    }

    @SuppressWarnings("unchecked")
    private StreamRule toStreamRule(DBObject dbObject) {
        final Map<String, Object> fields = dbObject.toMap();
        return new StreamRuleImpl((ObjectId) dbObject.get("_id"), fields);
    }
}
