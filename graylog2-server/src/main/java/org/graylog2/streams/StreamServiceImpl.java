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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationImpl;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.Alert;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.mongojack.DBProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StreamServiceImpl extends PersistedServiceImpl implements StreamService {
    private static final Logger LOG = LoggerFactory.getLogger(StreamServiceImpl.class);
    private final StreamRuleService streamRuleService;
    private final AlertService alertService;
    private final OutputService outputService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory indexSetFactory;
    private final NotificationService notificationService;
    private final EntityOwnershipService entityOwnershipService;
    private final ClusterEventBus clusterEventBus;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;

    @Inject
    public StreamServiceImpl(MongoConnection mongoConnection,
                             StreamRuleService streamRuleService,
                             AlertService alertService,
                             OutputService outputService,
                             IndexSetService indexSetService,
                             MongoIndexSet.Factory indexSetFactory,
                             NotificationService notificationService,
                             EntityOwnershipService entityOwnershipService,
                             ClusterEventBus clusterEventBus,
                             AlarmCallbackConfigurationService alarmCallbackConfigurationService) {
        super(mongoConnection);
        this.streamRuleService = streamRuleService;
        this.alertService = alertService;
        this.outputService = outputService;
        this.indexSetService = indexSetService;
        this.indexSetFactory = indexSetFactory;
        this.notificationService = notificationService;
        this.entityOwnershipService = entityOwnershipService;
        this.clusterEventBus = clusterEventBus;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
    }

    @Nullable
    private IndexSet getIndexSet(DBObject dbObject) {
        return getIndexSet((String) dbObject.get(StreamImpl.FIELD_INDEX_SET_ID));
    }

    @Nullable
    private IndexSet getIndexSet(String id) {
        if (isNullOrEmpty(id)) {
            return null;
        }
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(id);
        return indexSetConfig.flatMap(c -> Optional.of(indexSetFactory.create(c))).orElse(null);
    }

    public Stream load(ObjectId id) throws NotFoundException {
        final DBObject o = get(StreamImpl.class, id);

        if (o == null) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }

        final List<StreamRule> streamRules = streamRuleService.loadForStreamId(id.toHexString());

        final Set<Output> outputs = loadOutputsForRawStream(o);

        @SuppressWarnings("unchecked")
        final Map<String, Object> fields = o.toMap();
        return new StreamImpl((ObjectId) o.get("_id"), fields, streamRules, outputs, getIndexSet(o));
    }

    @Override
    public Stream create(Map<String, Object> fields) {
        return new StreamImpl(fields, getIndexSet((String) fields.get(StreamImpl.FIELD_INDEX_SET_ID)));
    }

    @Override
    public Stream create(CreateStreamRequest cr, String userId) {
        Map<String, Object> streamData = Maps.newHashMap();
        streamData.put(StreamImpl.FIELD_TITLE, cr.title());
        streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description());
        streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, userId);
        streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC());
        streamData.put(StreamImpl.FIELD_CONTENT_PACK, cr.contentPack());
        streamData.put(StreamImpl.FIELD_MATCHING_TYPE, cr.matchingType().toString());
        streamData.put(StreamImpl.FIELD_DISABLED, false);
        streamData.put(StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, cr.removeMatchesFromDefaultStream());
        streamData.put(StreamImpl.FIELD_INDEX_SET_ID, cr.indexSetId());

        return create(streamData);
    }

    @Override
    public Stream load(String id) throws NotFoundException {
        try {
            return load(new ObjectId(id));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }
    }

    @Override
    public List<Stream> loadAllEnabled() {
        return loadAllEnabled(new HashMap<>());
    }

    public List<Stream> loadAllEnabled(Map<String, Object> additionalQueryOpts) {
        additionalQueryOpts.put(StreamImpl.FIELD_DISABLED, false);

        return loadAll(additionalQueryOpts);
    }

    @Override
    public List<Stream> loadAll() {
        return loadAll(Collections.emptyMap());
    }

    public List<Stream> loadAll(Map<String, Object> additionalQueryOpts) {
        final DBObject query = new BasicDBObject(additionalQueryOpts);
        return loadAll(query);
    }

    private List<Stream> loadAll(DBObject query) {
        final List<DBObject> results = query(StreamImpl.class, query);
        final List<String> streamIds = results.stream()
                .map(o -> o.get("_id").toString())
                .collect(Collectors.toList());
        final Map<String, List<StreamRule>> allStreamRules = streamRuleService.loadForStreamIds(streamIds);

        final ImmutableList.Builder<Stream> streams = ImmutableList.builder();

        final Map<String, IndexSet> indexSets = indexSetsForStreams(results);

        final Set<String> outputIds = results.stream()
                .map(this::outputIdsForRawStream)
                .flatMap(outputs -> outputs.stream().map(ObjectId::toHexString))
                .collect(Collectors.toSet());

        final Map<String, Output> outputsById = outputService.loadByIds(outputIds)
                .stream()
                .collect(Collectors.toMap(Output::getId, Function.identity()));


        for (DBObject o : results) {
            final ObjectId objectId = (ObjectId) o.get("_id");
            final String id = objectId.toHexString();
            final List<StreamRule> streamRules = allStreamRules.getOrDefault(id, Collections.emptyList());
            LOG.debug("Found {} rules for stream <{}>", streamRules.size(), id);

            final Set<Output> outputs = outputIdsForRawStream(o)
                    .stream()
                    .map(ObjectId::toHexString)
                    .map(outputId -> {
                        final Output output = outputsById.get(outputId);
                        if (output == null) {
                            final String streamTitle = Strings.nullToEmpty((String) o.get(StreamImpl.FIELD_TITLE));
                            LOG.warn("Stream \"" + streamTitle + "\" <" + id + "> references missing output <" + outputId + "> - ignoring output.");
                        }
                        return output;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            @SuppressWarnings("unchecked")
            final Map<String, Object> fields = o.toMap();

            final String indexSetId = (String) fields.get(StreamImpl.FIELD_INDEX_SET_ID);

            streams.add(new StreamImpl(objectId, fields, streamRules, outputs, indexSets.get(indexSetId)));
        }

        return streams.build();
    }

    private List<ObjectId> outputIdsForRawStream(DBObject o) {
        final List<ObjectId> objectIds = (List<ObjectId>) o.get(StreamImpl.FIELD_OUTPUTS);
        return objectIds == null ? Collections.emptyList() : objectIds;
    }

    private Map<String, IndexSet> indexSetsForStreams(List<DBObject> streams) {
        final Set<String> indexSetIds = streams.stream()
                .map(stream -> (String) stream.get(StreamImpl.FIELD_INDEX_SET_ID))
                .filter(s -> !isNullOrEmpty(s))
                .collect(Collectors.toSet());
        return indexSetService.findByIds(indexSetIds)
                .stream()
                .collect(Collectors.toMap(IndexSetConfig::id, indexSetFactory::create));
    }

    @Override
    public Set<Stream> loadByIds(Collection<String> streamIds) {
        final Set<ObjectId> objectIds = streamIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        final DBObject query = QueryBuilder.start("_id").in(objectIds).get();

        return ImmutableSet.copyOf(loadAll(query));
    }

    @Override
    public Set<String> indexSetIdsByIds(Collection<String> streamIds) {
        final Set<ObjectId> objectIds = streamIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        final DBObject query = QueryBuilder.start("_id").in(objectIds).get();
        final DBObject onlyIndexSetIdField = DBProjection.include(StreamImpl.FIELD_INDEX_SET_ID);
        return StreamSupport.stream(collection(StreamImpl.class).find(query, onlyIndexSetIdField).spliterator(), false)
                .map(s -> s.get(StreamImpl.FIELD_INDEX_SET_ID).toString())
                .collect(Collectors.toSet());
    }

    @Override
    public List<Stream> loadAllWithConfiguredAlertConditions() {
        final DBObject query = QueryBuilder.start().and(
                QueryBuilder.start(StreamImpl.EMBEDDED_ALERT_CONDITIONS).exists(true).get(),
                QueryBuilder.start(StreamImpl.EMBEDDED_ALERT_CONDITIONS).not().size(0).get()
        ).get();

        return loadAll(query);
    }

    protected Set<Output> loadOutputsForRawStream(DBObject stream) {
        List<ObjectId> outputIds = outputIdsForRawStream(stream);

        Set<Output> result = new HashSet<>();
        if (outputIds != null) {
            for (ObjectId outputId : outputIds) {
                try {
                    result.add(outputService.load(outputId.toHexString()));
                } catch (NotFoundException e) {
                    LOG.warn("Non-existing output <{}> referenced from stream <{}>!", outputId.toHexString(), stream.get("_id"));
                }
            }
        }

        return result;
    }

    @Override
    public long count() {
        return totalCount(StreamImpl.class);
    }

    @Override
    public void destroy(Stream stream) throws NotFoundException {
        for (StreamRule streamRule : streamRuleService.loadForStream(stream)) {
            super.destroy(streamRule);
        }

        final String streamId = stream.getId();
        for (Notification notification : notificationService.all()) {
            Object rawValue = notification.getDetail("stream_id");
            if (rawValue != null && rawValue.toString().equals(streamId)) {
                LOG.debug("Removing notification that references stream: {}", notification);
                notificationService.destroy(notification);
            }
        }
        super.destroy(stream);

        clusterEventBus.post(StreamsChangedEvent.create(streamId));
        clusterEventBus.post(StreamDeletedEvent.create(streamId));
        entityOwnershipService.unregisterStream(streamId);
    }

    public void update(Stream stream, String title, String description) throws ValidationException {
        if (title != null) {
            stream.getFields().put(StreamImpl.FIELD_TITLE, title);
        }

        if (description != null) {
            stream.getFields().put(StreamImpl.FIELD_DESCRIPTION, description);
        }

        save(stream);
    }

    @Override
    public void pause(Stream stream) throws ValidationException {
        stream.setDisabled(true);
        final String streamId = save(stream);
        clusterEventBus.post(StreamsChangedEvent.create(streamId));
    }

    @Override
    public void resume(Stream stream) throws ValidationException {
        stream.setDisabled(false);
        final String streamId = save(stream);
        clusterEventBus.post(StreamsChangedEvent.create(streamId));
    }

    @Override
    public List<StreamRule> getStreamRules(Stream stream) throws NotFoundException {
        return streamRuleService.loadForStream(stream);
    }

    @Override
    public List<AlertCondition> getAlertConditions(Stream stream) {
        List<AlertCondition> conditions = Lists.newArrayList();

        if (stream.getFields().containsKey(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
            @SuppressWarnings("unchecked")
            final List<BasicDBObject> alertConditions = (List<BasicDBObject>) stream.getFields().get(StreamImpl.EMBEDDED_ALERT_CONDITIONS);
            for (BasicDBObject conditionFields : alertConditions) {
                try {
                    conditions.add(alertService.fromPersisted(conditionFields, stream));
                } catch (Exception e) {
                    LOG.error("Skipping alert condition.", e);
                }
            }
        }

        return conditions;
    }

    @Override
    public AlertCondition getAlertCondition(Stream stream, String conditionId) throws NotFoundException {
        if (stream.getFields().containsKey(StreamImpl.EMBEDDED_ALERT_CONDITIONS)) {
            @SuppressWarnings("unchecked")
            final List<BasicDBObject> alertConditions = (List<BasicDBObject>) stream.getFields().get(StreamImpl.EMBEDDED_ALERT_CONDITIONS);
            for (BasicDBObject conditionFields : alertConditions) {
                try {
                    if (conditionFields.get("id").equals(conditionId)) {
                        return alertService.fromPersisted(conditionFields, stream);
                    }
                } catch (Exception e) {
                    LOG.error("Skipping alert condition.", e);
                }
            }
        }

        throw new NotFoundException("Alert condition <" + conditionId + "> for stream <" + stream.getId() + "> not found");
    }

    @Override
    public void addAlertCondition(Stream stream, AlertCondition condition) throws ValidationException {
        embed(stream, StreamImpl.EMBEDDED_ALERT_CONDITIONS, (EmbeddedPersistable) condition);
    }

    @Override
    public void updateAlertCondition(Stream stream, AlertCondition condition) throws ValidationException {
        removeAlertCondition(stream, condition.getId());
        addAlertCondition(stream, condition);
    }

    @Override
    public void removeAlertCondition(Stream stream, String conditionId) {
        // Before deleting alert condition, resolve all its alerts.
        final List<Alert> alerts = alertService.listForStreamIds(Collections.singletonList(stream.getId()), Alert.AlertState.UNRESOLVED, 0, 0);
        alerts.stream()
                .filter(alert -> alert.getConditionId().equals(conditionId))
                .forEach(alertService::resolveAlert);

        removeEmbedded(stream, StreamImpl.EMBEDDED_ALERT_CONDITIONS, conditionId);
    }

    @Override
    public void addAlertReceiver(Stream stream, String type, String name) {
        final List<AlarmCallbackConfiguration> streamCallbacks = alarmCallbackConfigurationService.getForStream(stream);
        updateCallbackConfiguration("add", type, name, streamCallbacks);
    }

    @Override
    public void removeAlertReceiver(Stream stream, String type, String name) {
        final List<AlarmCallbackConfiguration> streamCallbacks = alarmCallbackConfigurationService.getForStream(stream);
        updateCallbackConfiguration("remove", type, name, streamCallbacks);
    }

    // I tried to be sorry, really. https://www.youtube.com/watch?v=3KVyRqloGmk
    private void updateCallbackConfiguration(String action, String type, String entity, List<AlarmCallbackConfiguration> streamCallbacks) {
        final AtomicBoolean ran = new AtomicBoolean(false);

        streamCallbacks.stream()
                .filter(callback -> callback.getType().equals(EmailAlarmCallback.class.getCanonicalName()))
                .forEach(callback -> {
                    ran.set(true);
                    final Map<String, Object> configuration = callback.getConfiguration();
                    String key;

                    if ("users".equals(type)) {
                        key = EmailAlarmCallback.CK_USER_RECEIVERS;
                    } else {
                        key = EmailAlarmCallback.CK_EMAIL_RECEIVERS;
                    }

                    @SuppressWarnings("unchecked")
                    final List<String> recipients = (List<String>) configuration.get(key);
                    if ("add".equals(action)) {
                        if (!recipients.contains(entity)) {
                            recipients.add(entity);
                        }
                    } else {
                        if (recipients.contains(entity)) {
                            recipients.remove(entity);
                        }
                    }
                    configuration.put(key, recipients);

                    final AlarmCallbackConfiguration updatedConfig = ((AlarmCallbackConfigurationImpl) callback).toBuilder()
                            .setConfiguration(configuration)
                            .build();
                    try {
                        alarmCallbackConfigurationService.save(updatedConfig);
                    } catch (ValidationException e) {
                        throw new BadRequestException("Unable to save alarm callback configuration", e);
                    }
                });

        if (!ran.get()) {
            throw new BadRequestException("Unable to " + action + " receiver: Stream has no email alarm callback.");
        }
    }


    @Override
    public void addOutput(Stream stream, Output output) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$addToSet", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, new ObjectId(output.getId())))
        );
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @Override
    public void addOutputs(ObjectId streamId, Collection<ObjectId> outputIds) {
        final BasicDBList outputs = new BasicDBList();
        outputs.addAll(outputIds);

        collection(StreamImpl.class).update(
                new BasicDBObject("_id", streamId),
                new BasicDBObject("$addToSet", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, new BasicDBObject("$each", outputs)))
        );
        clusterEventBus.post(StreamsChangedEvent.create(streamId.toHexString()));
    }

    @Override
    public void removeOutput(Stream stream, Output output) {
        collection(stream).update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$pull", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, new ObjectId(output.getId())))
        );

        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @Override
    public void removeOutputFromAllStreams(Output output) {
        ObjectId outputId = new ObjectId(output.getId());
        DBObject match = new BasicDBObject(StreamImpl.FIELD_OUTPUTS, outputId);
        DBObject modify = new BasicDBObject("$pull", new BasicDBObject(StreamImpl.FIELD_OUTPUTS, outputId));

        // Collect streams that will change before updating them because we don't get the list of changed streams
        // from the upsert call.
        final ImmutableSet<String> updatedStreams;
        try (final DBCursor cursor = collection(StreamImpl.class).find(match)) {
            updatedStreams = StreamSupport.stream(cursor.spliterator(), false)
                    .map(stream -> stream.get("_id"))
                    .filter(Objects::nonNull)
                    .map(id -> ((ObjectId) id).toHexString())
                    .collect(ImmutableSet.toImmutableSet());
        }

        collection(StreamImpl.class).update(
                match, modify, false, true
        );

        clusterEventBus.post(StreamsChangedEvent.create(updatedStreams));
    }

    @Override
    public List<Stream> loadAllWithIndexSet(String indexSetId) {
        final Map<String, Object> query = new BasicDBObject(StreamImpl.FIELD_INDEX_SET_ID, indexSetId);
        return loadAll(query);
    }

    @Override
    public String save(Stream stream) throws ValidationException {
        final String savedStreamId = super.save(stream);
        clusterEventBus.post(StreamsChangedEvent.create(savedStreamId));

        return savedStreamId;
    }

    @Override
    public String saveWithRulesAndOwnership(Stream stream, Collection<StreamRule> streamRules, User user) throws ValidationException {
        final String savedStreamId = super.save(stream);
        final Set<StreamRule> rules = streamRules.stream()
                .map(rule -> streamRuleService.copy(savedStreamId, rule))
                .collect(Collectors.toSet());
        streamRuleService.save(rules);

        entityOwnershipService.registerNewStream(savedStreamId, user);
        clusterEventBus.post(StreamsChangedEvent.create(savedStreamId));

        return savedStreamId;
    }
}
