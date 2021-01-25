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
package org.graylog.events.legacy;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.events.conditions.Expr;
import org.graylog.events.conditions.Expression;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationConditions;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationFunction;
import org.graylog.events.processor.aggregation.AggregationSeries;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Takes care of migrating legacy alert condition and alarm callback configurations to new {@link org.graylog.events.processor.EventDefinition event definitions}
 * and {@link org.graylog.events.notifications.EventNotificationConfig notification configurations}.
 *
 * This class is deliberately avoiding the usage of the legacy Java classes by using the raw MongoDB client
 * so we can safely delete the legacy classes at one point and still run the migrations.
 */
public class LegacyAlertConditionMigrator {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyAlertConditionMigrator.class);

    private final MongoCollection<Document> streamsCollection;
    private final MongoCollection<Document> alarmCallbacksCollection;
    private final EventDefinitionHandler eventDefinitionHandler;
    private final NotificationResourceHandler notificationResourceHandler;
    private final DBNotificationService dbNotificationService;
    private final UserService userService;
    private final long executeEveryMs;

    @Inject
    public LegacyAlertConditionMigrator(MongoConnection mongoConnection,
                                        EventDefinitionHandler eventDefinitionHandler,
                                        NotificationResourceHandler notificationResourceHandler,
                                        DBNotificationService dbNotificationService,
                                        UserService userService,
                                        @Named("alert_check_interval") int alertCheckInterval) {
        this.streamsCollection = mongoConnection.getMongoDatabase().getCollection("streams");
        this.alarmCallbacksCollection = mongoConnection.getMongoDatabase().getCollection("alarmcallbackconfigurations");
        this.eventDefinitionHandler = eventDefinitionHandler;
        this.notificationResourceHandler = notificationResourceHandler;
        this.dbNotificationService = dbNotificationService;
        this.userService = userService;

        // The old alert conditions have been executed every "alert_check_interval" in seconds
        this.executeEveryMs = alertCheckInterval * 1000L;
    }

    public MigrationResult run(Set<String> completedAlertConditions, Set<String> completedAlarmCallbacks) {
        final MigrationResult.Builder result = MigrationResult.builder();

        streamsCollection.find().forEach((Block<Document>) stream -> {
            final String streamId = stream.getObjectId("_id").toHexString();
            final String streamTitle = stream.getString("title");

            final FindIterable<Document> iterable = alarmCallbacksCollection.find(Filters.eq("stream_id", streamId));
            final Set<NotificationDto> notifications = Streams.stream(iterable)
                    .map(alarmCallback -> {
                        final String callbackId = alarmCallback.getObjectId("_id").toHexString();

                        if (completedAlarmCallbacks.contains(callbackId)) {
                            result.addCompletedAlarmCallback(callbackId);
                            return dbNotificationService.get(callbackId).orElse(null);
                        }

                        try {
                            final NotificationDto notificationDto = migrateAlarmCallback(alarmCallback);
                            result.addCompletedAlarmCallback(callbackId);
                            return notificationDto;
                        } catch (Exception e) {
                            LOG.error("Couldn't migrate legacy alarm callback on stream <{}/{}>: {}", streamTitle, streamId, alarmCallback, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!stream.containsKey("alert_conditions")) {
                return;
            }

            @SuppressWarnings("unchecked")
            final List<Document> list = (List<Document>) stream.get("alert_conditions");

            list.forEach(alertCondition -> {
                final String conditionId = alertCondition.getString("id");
                final String conditionType = alertCondition.getString("type");

                if (completedAlertConditions.contains(conditionId)) {
                    result.addCompletedAlertCondition(conditionId);
                    return;
                }
                try {
                    switch (conditionType) {
                        case "message_count":
                            migrateMessageCount(new Helper(stream, alertCondition, notifications));
                            result.addCompletedAlertCondition(conditionId);
                            break;
                        case "field_value":
                            migrateFieldValue(new Helper(stream, alertCondition, notifications));
                            result.addCompletedAlertCondition(conditionId);
                            break;
                        case "field_content_value":
                            migrateFieldContentValue(new Helper(stream, alertCondition, notifications));
                            result.addCompletedAlertCondition(conditionId);
                            break;
                        default:
                            LOG.warn("Couldn't migrate unknown legacy alert condition type: {}", conditionType);
                    }
                } catch (Exception e) {
                    LOG.error("Couldn't migrate legacy alert condition on stream <{}/{}>: {}", streamTitle, streamId, alertCondition, e);
                }
            });
        });

        return result.build();
    }

    /**
     * Example alarm callback data structure:
     * <pre>{@code
     *     {
     *       "_id": "54e3deadbeefdeadbeef0001",
     *       "stream_id" : "54e3deadbeefdeadbeef0001",
     *       "type" : "org.graylog2.alarmcallbacks.HTTPAlarmCallback",
     *       "title" : "HTTP Callback Test",
     *       "configuration" : {
     *         "url" : "http://localhost:11000/"
     *       },
     *       "created_at": "2019-01-01T00:00:00.000Z",
     *       "creator_user_id" : "admin"
     *     }
     * }</pre>
     */
    private NotificationDto migrateAlarmCallback(Document alarmCallback) {
        final String title = alarmCallback.getString("title");
        final String type = alarmCallback.getString("type");
        final Document configDoc = (Document) alarmCallback.get("configuration");
        final Map<String, Object> configuration = configDoc.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final LegacyAlarmCallbackEventNotificationConfig config = LegacyAlarmCallbackEventNotificationConfig.builder()
                .callbackType(type)
                .configuration(configuration)
                .build();

        final NotificationDto dto = NotificationDto.builder()
                .title(firstNonNull(title, "Untitled"))
                .description("Migrated legacy alarm callback")
                .config(config)
                .build();

        LOG.info("Migrate legacy alarm callback <{}>", dto.title());
        return notificationResourceHandler.create(dto, userService.getRootUser());
    }

    /**
     * Example message count alert condition data structure on streams:
     * <pre>{@code
     *         {
     *           "id" : "00000000-0000-0000-0000-000000000001",
     *           "type" : "message_count",
     *           "title" : "Message Count - MORE",
     *           "parameters" : {
     *             "backlog" : 10,
     *             "repeat_notifications" : false,
     *             "query" : "hello:world",
     *             "grace" : 2,
     *             "threshold_type" : "MORE",
     *             "threshold" : 1,
     *             "time" : 10
     *           },
     *           "creator_user_id" : "admin",
     *           "created_at": "2019-01-01T00:00:00.000Z"
     *         }
     * }</pre>
     */
    private void migrateMessageCount(Helper helper) {
        final String seriesId = helper.newSeriesId();

        final AggregationSeries messageCountSeries = AggregationSeries.builder()
                .id(seriesId)
                .function(AggregationFunction.COUNT)
                .field(null)
                .build();

        final Expression<Boolean> expression = helper.createExpression(seriesId, "MORE");
        final EventProcessorConfig config = helper.createAggregationProcessorConfig(messageCountSeries, expression, executeEveryMs);
        final EventDefinitionDto definitionDto = helper.createEventDefinition(config);

        LOG.info("Migrate legacy message count alert condition <{}>", definitionDto.title());
        eventDefinitionHandler.create(definitionDto, userService.getRootUser());
    }

    /**
     * Example field value alert condition data structure on streams:
     * <pre>{@code
     *         {
     *           "id" : "00000000-0000-0000-0000-000000000001",
     *           "type" : "field_value",
     *           "title" : "Field Value - HIGHER - MEAN",
     *           "parameters" : {
     *             "backlog" : 15,
     *             "repeat_notifications" : false,
     *             "field" : "test_field_1",
     *             "query" : "*",
     *             "grace" : 1,
     *             "threshold_type" : "HIGHER",
     *             "threshold" : 23,
     *             "time" : 5,
     *             "type" : "MEAN"
     *           },
     *           "creator_user_id" : "admin",
     *           "created_at": "2019-01-01T00:00:00.000Z"
     *         }
     * }</pre>
     */
    private void migrateFieldValue(Helper helper) {
        final String type = helper.parameters().getString("type");
        final String field = helper.parameters().getString("field");

        final String seriesId = helper.newSeriesId();

        final AggregationSeries.Builder aggregationSeriesBuilder = AggregationSeries.builder()
                .id(seriesId)
                .field(field);

        switch (type.toUpperCase(Locale.US)) {
            case "MEAN":
                aggregationSeriesBuilder.function(AggregationFunction.AVG);
                break;
            case "MIN":
                aggregationSeriesBuilder.function(AggregationFunction.MIN);
                break;
            case "MAX":
                aggregationSeriesBuilder.function(AggregationFunction.MAX);
                break;
            case "SUM":
                aggregationSeriesBuilder.function(AggregationFunction.SUM);
                break;
            case "STDDEV":
                aggregationSeriesBuilder.function(AggregationFunction.STDDEV);
                break;
            default:
                LOG.warn("Couldn't migrate field value alert condition with unknown type: {}", type);
                return;
        }

        final AggregationSeries aggregationSeries = aggregationSeriesBuilder.build();
        final Expression<Boolean> expression = helper.createExpression(seriesId, "HIGHER");
        final EventProcessorConfig config = helper.createAggregationProcessorConfig(aggregationSeries, expression, executeEveryMs);
        final EventDefinitionDto definitionDto = helper.createEventDefinition(config);

        LOG.info("Migrate legacy field value alert condition <{}>", definitionDto.title());
        eventDefinitionHandler.create(definitionDto, userService.getRootUser());
    }

    /**
     * Example field content value alert condition data structure on streams:
     * <pre>{@code
     *         {
     *           "id" : "00000000-0000-0000-0000-000000000001",
     *           "type" : "field_content_value",
     *           "title" : "Field Content - WITHOUT QUERY",
     *           "parameters" : {
     *             "backlog" : 100,
     *             "repeat_notifications" : false,
     *             "field" : "test_field_2",
     *             "query" : "",
     *             "grace" : 2,
     *             "value" : "hello"
     *           },
     *           "creator_user_id" : "admin",
     *           "created_at": "2019-01-01T00:00:00.000Z"
     *         }
     * }</pre>
     */
    private void migrateFieldContentValue(Helper helper) {
        final String field = helper.parameters().getString("field");
        final String value = helper.parameters().getString("value");

        // The configured condition query can be empty
        String query = field + ":\"" + value + "\"";
        if (!isNullOrEmpty(helper.query) && !"*".equals(helper.query.trim())) {
            query = query + " AND " + helper.query;
        }

        final String seriesId = helper.newSeriesId();

        final AggregationSeries messageCountSeries = AggregationSeries.builder()
                .id(seriesId)
                .function(AggregationFunction.COUNT)
                .field(null)
                .build();

        final Expr.NumberReference left = Expr.NumberReference.create(seriesId);
        final Expr.NumberValue right = Expr.NumberValue.create(0);
        final Expression<Boolean> expression = Expr.Greater.create(left, right);

        final EventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .streams(ImmutableSet.of(helper.streamId))
                .query(query)
                .series(ImmutableList.of(messageCountSeries))
                .groupBy(ImmutableList.of())
                .conditions(AggregationConditions.builder()
                        .expression(expression)
                        .build())
                .searchWithinMs(executeEveryMs) // The FieldContentValueAlertCondition was just using the alert scanner interval
                .executeEveryMs(executeEveryMs)
                .build();

        final EventDefinitionDto definitionDto = helper.createEventDefinition(config);

        LOG.info("Migrate legacy field content value alert condition <{}>", definitionDto.title());
        eventDefinitionHandler.create(definitionDto, userService.getRootUser());
    }

    private static class Helper {
        private final String streamId;
        private final String title;
        private final Document parameters;
        private final Set<NotificationDto> notifications;
        private final long gracePeriod;
        private final long backlogSize;
        private final String query;
        private final long time;
        private final long threshold;
        private final String thresholdType;

        Helper(Document stream, Document alertCondition, Set<NotificationDto> notifications) {
            streamId = stream.getObjectId("_id").toHexString();
            title = alertCondition.getString("title");
            parameters = (Document) alertCondition.get("parameters");
            this.notifications = notifications;
            gracePeriod = firstNonNull((Number) parameters.get("grace"), 0).longValue();
            backlogSize = firstNonNull((Number) parameters.get("backlog"), 0).longValue();
            query = parameters.get("query", "");
            time = firstNonNull((Number) parameters.get("time"), 0).longValue();
            threshold = firstNonNull((Number) parameters.get("threshold"), 0).longValue();
            thresholdType = parameters.get("threshold_type", "MORE");
        }

        Document parameters() {
            return parameters;
        }

        EventProcessorConfig createAggregationProcessorConfig(AggregationSeries aggregationSeries, Expression<Boolean> expression, long executeEveryMs) {
            return AggregationEventProcessorConfig.builder()
                    .streams(ImmutableSet.of(streamId))
                    .query(query)
                    .series(ImmutableList.of(aggregationSeries))
                    .groupBy(ImmutableList.of())
                    .conditions(AggregationConditions.builder()
                            .expression(expression)
                            .build())
                    .searchWithinMs(time * 60 * 1000)
                    .executeEveryMs(executeEveryMs)
                    .build();
        }

        Expression<Boolean> createExpression(String seriesId, String greaterValue) {
            final Expr.NumberReference left = Expr.NumberReference.create(seriesId);
            final Expr.NumberValue right = Expr.NumberValue.create(threshold);

            return greaterValue.equalsIgnoreCase(thresholdType) ? Expr.Greater.create(left, right) : Expr.Lesser.create(left, right);
        }

        EventDefinitionDto createEventDefinition(EventProcessorConfig config) {
            final ImmutableList<EventNotificationHandler.Config> notificationList = notifications.stream()
                    .map(notification -> EventNotificationHandler.Config.builder()
                            .notificationId(notification.id())
                            .build())
                    .collect(ImmutableList.toImmutableList());

            return EventDefinitionDto.builder()
                    .title(firstNonNull(title, "Untitled"))
                    .description("Migrated message count alert condition")
                    .config(config)
                    .alert(true)
                    .priority(2)
                    .keySpec(ImmutableList.of())
                    .notificationSettings(EventNotificationSettings.builder()
                            .gracePeriodMs(gracePeriod * 60 * 1000)
                            .backlogSize(backlogSize)
                            .build())
                    .notifications(notificationList)
                    .build();
        }

        String newSeriesId() {
            return UUID.randomUUID().toString();
        }
    }

    @AutoValue
    public static abstract class MigrationResult {
        public abstract ImmutableSet<String> completedAlertConditions();

        public abstract ImmutableSet<String> completedAlarmCallbacks();

        public static Builder builder() {
            return new AutoValue_LegacyAlertConditionMigrator_MigrationResult.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            abstract ImmutableSet.Builder<String> completedAlertConditionsBuilder();

            abstract ImmutableSet.Builder<String> completedAlarmCallbacksBuilder();

            public Builder addCompletedAlertCondition(String id) {
                completedAlertConditionsBuilder().add(id);
                return this;
            }

            public Builder addCompletedAlarmCallback(String id) {
                completedAlarmCallbacksBuilder().add(id);
                return this;
            }

            public abstract MigrationResult build();
        }
    }
}
