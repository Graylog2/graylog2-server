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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationImpl;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class V20161125161400_AlertReceiversMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161125161400_AlertReceiversMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final StreamService streamService;
    private final AlarmCallbackConfigurationService alarmCallbackService;
    private final DBCollection dbCollection;

    @Inject
    public V20161125161400_AlertReceiversMigration(ClusterConfigService clusterConfigService,
                                                   StreamService streamService,
                                                   AlarmCallbackConfigurationService alarmCallbackService,
                                                   MongoConnection mongoConnection) {
        this.clusterConfigService = clusterConfigService;
        this.streamService = streamService;
        this.alarmCallbackService = alarmCallbackService;
        final String collectionName = StreamImpl.class.getAnnotation(CollectionName.class).value();
        this.dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-25T16:14:00Z");
    }

    private boolean hasAlertReceivers(Stream stream) {
        final Map<String, List<String>> alertReceivers = stream.getAlertReceivers();
        if (alertReceivers == null || alertReceivers.isEmpty()) {
            return false;
        }

        final List<String> users = alertReceivers.get("users");
        final List<String> emails = alertReceivers.get("emails");
        return users != null && !users.isEmpty() || emails != null && !emails.isEmpty();
    }

    @Override
    public void upgrade() {
        // Do not run again if the migration marker can be found in the database.
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            return;
        }

        final Map<String, Optional<String>> streamMigrations = this.streamService.loadAll()
                .stream()
                .filter(stream -> this.hasAlertReceivers(stream)
                        && !streamService.getAlertConditions(stream).isEmpty()
                        && !alarmCallbackService.getForStream(stream).isEmpty())
                .collect(Collectors.toMap(Persisted::getId, this::migrateStream));
        final boolean allSucceeded = streamMigrations.values()
                .stream()
                .allMatch(Optional::isPresent);

        final long count = streamMigrations.size();
        if (allSucceeded) {
            if (count > 0) {
                LOG.info("Successfully migrated alert receivers from " + count + " streams to its email alarm callbacks.");
            } else {
                LOG.info("No streams needed to be migrated.");
            }
            this.clusterConfigService.write(MigrationCompleted.create(streamMigrations));
        } else {
            final long errors = streamMigrations.values()
                    .stream()
                    .filter(streamId -> !streamId.isPresent())
                    .count();
            LOG.error("Failed migrating " + errors + "/" + count + " alert receivers from streams to its email alarm callbacks.");
        }
    }

    private Optional<String> migrateStream(Stream stream) {
        final List<AlarmCallbackConfiguration> alarmCallbacks = alarmCallbackService.getForStream(stream);
        final List<Optional<String>> updatedConfigurations = alarmCallbacks.stream()
                .filter(callbackConfiguration -> callbackConfiguration.getType().equals(EmailAlarmCallback.class.getCanonicalName()))
                .map(callbackConfiguration -> this.updateConfiguration(stream, callbackConfiguration))
                .collect(Collectors.toList());

        if (!updatedConfigurations.stream().allMatch(Optional::isPresent)) {
            final long errors = updatedConfigurations.stream()
                    .filter(streamId -> !streamId.isPresent())
                    .count();
            LOG.error("Failed moving alert receivers in " + errors + " email alarm callbacks.");
            return Optional.empty();
        }

        this.dbCollection.update(
                new BasicDBObject("_id", new ObjectId(stream.getId())),
                new BasicDBObject("$unset", new BasicDBObject(StreamImpl.FIELD_ALERT_RECEIVERS, ""))
        );
        LOG.debug("Successfully removed alert receivers from stream <" + stream.getId() + ">.");

        return Optional.of(updatedConfigurations.stream().map(Optional::get).collect(Collectors.joining(", ")));
    }

    private Optional<String> updateConfiguration(Stream stream, AlarmCallbackConfiguration callbackConfiguration) {
        final Map<String, List<String>> alertReceivers = stream.getAlertReceivers();
        final List<String> usernames = alertReceivers.get("users");
        final List<String> emails = alertReceivers.get("emails");

        final Map<String, Object> configuration = callbackConfiguration.getConfiguration();

        if (usernames != null && !usernames.isEmpty()) {
            LOG.debug("Moving users alert receivers from stream <" + stream.getId() + ">");
            configuration.put(EmailAlarmCallback.CK_USER_RECEIVERS, usernames);
        }

        if (emails != null && !emails.isEmpty()) {
            LOG.debug("Moving emails alert receivers from stream <" + stream.getId() + ">");
            configuration.put(EmailAlarmCallback.CK_EMAIL_RECEIVERS, emails);
        }

        final AlarmCallbackConfigurationImpl updatedConfiguration = ((AlarmCallbackConfigurationImpl) callbackConfiguration).toBuilder()
                .setConfiguration(configuration).build();

        try {
            final String callbackId = this.alarmCallbackService.save(updatedConfiguration);
            LOG.debug("Successfully created email alarm callback <" + callbackId + "> for stream <" + stream.getId() + ">.");
            return Optional.of(callbackId);
        } catch (ValidationException e) {
            LOG.error("Unable to create email alarm callback for stream <" + stream.getId() + ">: ", e);
        }

        return Optional.empty();
    }

    @AutoValue
    @WithBeanGetter
    @JsonAutoDetect
    public static abstract class MigrationCompleted {
        @JsonProperty("stream_ids")
        public abstract Map<String, Optional<String>> streamIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("stream_ids") Map<String, Optional<String>> streamIds) {
            return new AutoValue_V20161125161400_AlertReceiversMigration_MigrationCompleted(streamIds);
        }
    }
}
