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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.events.conditions.Expr;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.EventNotification;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.NotificationResourceHandler;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventDefinitionHandler;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationEventProcessorParameters;
import org.graylog.events.processor.aggregation.AggregationFunction;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.users.UserService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LegacyAlertConditionMigratorTest {
    private static final int CHECK_INTERVAL = 60;

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private LegacyAlertConditionMigrator migrator;

    private DBEventDefinitionService eventDefinitionService;
    private EventDefinitionHandler eventDefinitionHandler;
    private DBNotificationService notificationService;
    private NotificationResourceHandler notificationResourceHandler;
    private UserService userService;

    @Mock
    private Map<String, EventNotification.Factory> eventNotificationFactories;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        objectMapper.registerSubtypes(new NamedType(AggregationEventProcessorConfig.class, AggregationEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(AggregationEventProcessorParameters.class, AggregationEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventProcessorExecutionJob.Config.class, EventProcessorExecutionJob.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventProcessorExecutionJob.Data.class, EventProcessorExecutionJob.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(IntervalJobSchedule.class, IntervalJobSchedule.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(LegacyAlarmCallbackEventNotificationConfig.class, LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(EventNotificationExecutionJob.Config.class, EventNotificationExecutionJob.TYPE_NAME));

        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final JobSchedulerTestClock clock = new JobSchedulerTestClock(DateTime.now(DateTimeZone.UTC));
        final DBJobDefinitionService jobDefinitionService = new DBJobDefinitionService(mongoConnection, mongoJackObjectMapperProvider);
        final DBJobTriggerService jobTriggerService = new DBJobTriggerService(mongoConnection, mongoJackObjectMapperProvider, mock(NodeId.class), clock);
        notificationService = new DBNotificationService(mongoConnection, mongoJackObjectMapperProvider, mock(EntityOwnershipService.class));

        this.eventDefinitionService = new DBEventDefinitionService(mongoConnection, mongoJackObjectMapperProvider, mock(DBEventProcessorStateService.class), mock(EntityOwnershipService.class));
        this.eventDefinitionHandler = spy(new EventDefinitionHandler(eventDefinitionService, jobDefinitionService, jobTriggerService, clock));
        this.notificationResourceHandler = spy(new NotificationResourceHandler(notificationService, jobDefinitionService, eventDefinitionService, eventNotificationFactories));
        this.userService = mock(UserService.class);
        when(userService.getRootUser()).thenReturn(Optional.empty());

        this.migrator = new LegacyAlertConditionMigrator(mongoConnection, eventDefinitionHandler, notificationResourceHandler, notificationService, userService, CHECK_INTERVAL);
    }

    @Test
    @MongoDBFixtures("legacy-alert-conditions.json")
    public void run() {
        final int migratedConditions = 10;
        final int migratedCallbacks = 4;

        assertThat(migrator.run(Collections.emptySet(), Collections.emptySet())).satisfies(result -> {
            assertThat(result.completedAlertConditions()).containsOnly(
                    "00000000-0000-0000-0000-000000000001",
                    "00000000-0000-0000-0000-000000000002",
                    "00000000-0000-0000-0000-000000000003",
                    "00000000-0000-0000-0000-000000000004",
                    "00000000-0000-0000-0000-000000000005",
                    "00000000-0000-0000-0000-000000000006",
                    "00000000-0000-0000-0000-000000000007",
                    "00000000-0000-0000-0000-000000000008",
                    "00000000-0000-0000-0000-000000000009",
                    "00000000-0000-0000-0000-000000000010"
            );
            assertThat(result.completedAlarmCallbacks()).containsOnly(
                    "54e3deadbeefdeadbeef0001",
                    "54e3deadbeefdeadbeef0002",
                    "54e3deadbeefdeadbeef0003",
                    "54e3deadbeefdeadbeef0004"
            );
        });

        // Make sure we use the EventDefinitionHandler to create the event definitions
        verify(eventDefinitionHandler, times(migratedConditions)).create(any(EventDefinitionDto.class), any(Optional.class));

        // Make sure we use the NotificationResourceHandler to create the notifications
        verify(notificationResourceHandler, times(migratedCallbacks)).create(any(NotificationDto.class), any(Optional.class));

        assertThat(eventDefinitionService.streamAll().count()).isEqualTo(migratedConditions);
        assertThat(notificationService.streamAll().count()).isEqualTo(migratedCallbacks);

        final NotificationDto httpNotification = notificationService.streamAll()
                .filter(n -> n.title().equals("HTTP Callback Test"))
                .findFirst()
                .orElse(null);

        assertThat(httpNotification).isNotNull();
        assertThat(httpNotification.title()).isEqualTo("HTTP Callback Test");
        assertThat(httpNotification.description()).isEqualTo("Migrated legacy alarm callback");
        assertThat(httpNotification.config()).isInstanceOf(LegacyAlarmCallbackEventNotificationConfig.class);
        assertThat((LegacyAlarmCallbackEventNotificationConfig) httpNotification.config()).satisfies(config -> {
            assertThat(config.callbackType()).isEqualTo("org.graylog2.alarmcallbacks.HTTPAlarmCallback");
            assertThat(config.configuration().get("url")).isEqualTo("http://localhost:11000/");
        });

        final NotificationDto httpNotificationWithoutTitle = notificationService.streamAll()
                .filter(n -> n.title().equals("Untitled"))
                .findFirst()
                .orElse(null);

        assertThat(httpNotificationWithoutTitle).isNotNull();
        assertThat(httpNotificationWithoutTitle.title()).isEqualTo("Untitled");
        assertThat(httpNotificationWithoutTitle.description()).isEqualTo("Migrated legacy alarm callback");
        assertThat(httpNotificationWithoutTitle.config()).isInstanceOf(LegacyAlarmCallbackEventNotificationConfig.class);
        assertThat((LegacyAlarmCallbackEventNotificationConfig) httpNotificationWithoutTitle.config()).satisfies(config -> {
            assertThat(config.callbackType()).isEqualTo("org.graylog2.alarmcallbacks.HTTPAlarmCallback");
            assertThat(config.configuration().get("url")).isEqualTo("http://localhost:11000/");
        });

        final NotificationDto emailNotification = notificationService.streamAll()
                .filter(n -> n.title().equals("Email Callback Test"))
                .findFirst()
                .orElse(null);

        assertThat(emailNotification).isNotNull();
        assertThat(emailNotification.title()).isEqualTo("Email Callback Test");
        assertThat(emailNotification.description()).isEqualTo("Migrated legacy alarm callback");
        assertThat(emailNotification.config()).isInstanceOf(LegacyAlarmCallbackEventNotificationConfig.class);
        assertThat((LegacyAlarmCallbackEventNotificationConfig) emailNotification.config()).satisfies(config -> {
            assertThat(config.callbackType()).isEqualTo("org.graylog2.alarmcallbacks.EmailAlarmCallback");
            assertThat(config.configuration().get("sender")).isEqualTo("graylog@example.org");
            assertThat(config.configuration().get("subject")).isEqualTo("Graylog alert for stream: ${stream.title}: ${check_result.resultDescription}");
            assertThat((String) config.configuration().get("body")).contains("Alert Description: ${check_result.resultDescription}\nDate: ");
            assertThat(config.configuration().get("user_receivers")).isEqualTo(Collections.emptyList());
            assertThat(config.configuration().get("email_receivers")).isEqualTo(Collections.singletonList("jane@example.org"));
        });

        final NotificationDto slackNotification = notificationService.streamAll()
                .filter(n -> n.title().equals("Slack Callback Test"))
                .findFirst()
                .orElse(null);

        assertThat(slackNotification).isNotNull();
        assertThat(slackNotification.title()).isEqualTo("Slack Callback Test");
        assertThat(slackNotification.description()).isEqualTo("Migrated legacy alarm callback");
        assertThat(slackNotification.config()).isInstanceOf(LegacyAlarmCallbackEventNotificationConfig.class);
        assertThat((LegacyAlarmCallbackEventNotificationConfig) slackNotification.config()).satisfies(config -> {
            assertThat(config.callbackType()).isEqualTo("org.graylog2.plugins.slack.callback.SlackAlarmCallback");
            assertThat(config.configuration().get("icon_url")).isEqualTo("");
            assertThat(config.configuration().get("graylog2_url")).isEqualTo("");
            assertThat(config.configuration().get("link_names")).isEqualTo(true);
            assertThat(config.configuration().get("webhook_url")).isEqualTo("http://example.com/slack-hook");
            assertThat(config.configuration().get("color")).isEqualTo("#FF0000");
            assertThat(config.configuration().get("icon_emoji")).isEqualTo("");
            assertThat(config.configuration().get("user_name")).isEqualTo("Graylog");
            assertThat(config.configuration().get("backlog_items")).isEqualTo(5);
            assertThat(config.configuration().get("custom_fields")).isEqualTo("");
            assertThat(config.configuration().get("proxy_address")).isEqualTo("");
            assertThat(config.configuration().get("channel")).isEqualTo("#channel");
            assertThat(config.configuration().get("notify_channel")).isEqualTo(false);
            assertThat(config.configuration().get("add_attachment")).isEqualTo(true);
            assertThat(config.configuration().get("short_mode")).isEqualTo(false);
        });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Message Count - MORE")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(120000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(10);

                    assertThat(eventDefinition.notifications()).hasSize(2);
                    assertThat(eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId).collect(Collectors.toList()))
                            .containsOnly(httpNotification.id(), httpNotificationWithoutTitle.id());

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0001");
                        assertThat(config.query()).isEqualTo("hello:world");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(10 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.COUNT);
                        assertThat(config.series().get(0).field()).isNotPresent();

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(1));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Message Count - LESS")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(0);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(0);

                    assertThat(eventDefinition.notifications()).hasSize(2);
                    assertThat(eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId).collect(Collectors.toList()))
                            .containsOnly(httpNotification.id(), httpNotificationWithoutTitle.id());

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0001");
                        assertThat(config.query()).isEmpty();
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(4 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.COUNT);
                        assertThat(config.series().get(0).field()).isNotPresent();

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Lesser.class);

                                final Expr.Lesser lesser = (Expr.Lesser) expression;

                                assertThat(lesser.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(lesser.right()).isEqualTo(Expr.NumberValue.create(42));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Value - HIGHER - MEAN")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(60000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(15);
                    assertThat(eventDefinition.notifications()).isEmpty();

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0002");
                        assertThat(config.query()).isEqualTo("*");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(5 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.AVG);
                        assertThat(config.series().get(0).field()).get().isEqualTo("test_field_1");

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(23));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Value - LOWER - SUM")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(60000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(15);
                    assertThat(eventDefinition.notifications()).isEmpty();

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0002");
                        assertThat(config.query()).isEqualTo("*");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(5 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.SUM);
                        assertThat(config.series().get(0).field()).get().isEqualTo("test_field_1");

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Lesser.class);

                                final Expr.Lesser lesser = (Expr.Lesser) expression;

                                assertThat(lesser.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(lesser.right()).isEqualTo(Expr.NumberValue.create(23));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Value - LOWER - MIN")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(60000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(15);
                    assertThat(eventDefinition.notifications()).isEmpty();

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0002");
                        assertThat(config.query()).isEqualTo("*");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(5 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.MIN);
                        assertThat(config.series().get(0).field()).get().isEqualTo("test_field_1");

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Lesser.class);

                                final Expr.Lesser lesser = (Expr.Lesser) expression;

                                assertThat(lesser.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(lesser.right()).isEqualTo(Expr.NumberValue.create(23));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Value - LOWER - MAX")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(60000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(15);
                    assertThat(eventDefinition.notifications()).isEmpty();

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0002");
                        assertThat(config.query()).isEqualTo("*");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(5 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.MAX);
                        assertThat(config.series().get(0).field()).get().isEqualTo("test_field_1");

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Lesser.class);

                                final Expr.Lesser lesser = (Expr.Lesser) expression;

                                assertThat(lesser.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(lesser.right()).isEqualTo(Expr.NumberValue.create(23));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Value - LOWER - STDDEV")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(60000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(15);
                    assertThat(eventDefinition.notifications()).isEmpty();

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0002");
                        assertThat(config.query()).isEqualTo("*");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(5 * 60 * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.STDDEV);
                        assertThat(config.series().get(0).field()).get().isEqualTo("test_field_1");

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(23));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Content - WITHOUT QUERY")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(120000);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(100);

                    assertThat(eventDefinition.notifications()).hasSize(2);
                    assertThat(eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId).collect(Collectors.toSet()))
                            .containsOnly(emailNotification.id(), slackNotification.id());

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0003");
                        assertThat(config.query()).isEqualTo("test_field_2:\"hello\"");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(CHECK_INTERVAL * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.COUNT);
                        assertThat(config.series().get(0).field()).isNotPresent();

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(0));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Field Content - WITH QUERY")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(0);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(0);

                    assertThat(eventDefinition.notifications()).hasSize(2);
                    assertThat(eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId).collect(Collectors.toSet()))
                            .containsOnly(emailNotification.id(), slackNotification.id());

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0003");
                        assertThat(config.query()).isEqualTo("test_field_3:\"foo\" AND foo:bar");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(CHECK_INTERVAL * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.COUNT);
                        assertThat(config.series().get(0).field()).isNotPresent();

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(0));
                            });
                        });
                    });
                });

        assertThat(eventDefinitionService.streamAll().filter(ed -> ed.title().equals("Untitled")).findFirst())
                .get()
                .satisfies(eventDefinition -> {
                    assertThat(eventDefinition.alert()).isTrue();
                    assertThat(eventDefinition.priority()).isEqualTo(2);
                    assertThat(eventDefinition.keySpec()).isEmpty();
                    assertThat(eventDefinition.notificationSettings().gracePeriodMs()).isEqualTo(0);
                    assertThat(eventDefinition.notificationSettings().backlogSize()).isEqualTo(0);

                    assertThat(eventDefinition.notifications()).hasSize(2);
                    assertThat(eventDefinition.notifications().stream().map(EventNotificationHandler.Config::notificationId).collect(Collectors.toSet()))
                            .containsOnly(emailNotification.id(), slackNotification.id());

                    assertThat((AggregationEventProcessorConfig) eventDefinition.config()).satisfies(config -> {
                        assertThat(config.streams()).containsExactly("54e3deadbeefdeadbeef0003");
                        assertThat(config.query()).isEqualTo("test_field_3:\"foo\" AND foo:bar");
                        assertThat(config.groupBy()).isEmpty();
                        assertThat(config.searchWithinMs()).isEqualTo(CHECK_INTERVAL * 1000);
                        assertThat(config.executeEveryMs()).isEqualTo(CHECK_INTERVAL * 1000);

                        assertThat(config.series()).hasSize(1);
                        assertThat(config.series().get(0).id()).isNotBlank();
                        assertThat(config.series().get(0).function()).isEqualTo(AggregationFunction.COUNT);
                        assertThat(config.series().get(0).field()).isNotPresent();

                        assertThat(config.conditions()).get().satisfies(conditions -> {
                            assertThat(conditions.expression()).get().satisfies(expression -> {
                                assertThat(expression).isInstanceOf(Expr.Greater.class);

                                final Expr.Greater greater = (Expr.Greater) expression;

                                assertThat(greater.left()).isEqualTo(Expr.NumberReference.create(config.series().get(0).id()));
                                assertThat(greater.right()).isEqualTo(Expr.NumberValue.create(0));
                            });
                        });
                    });
                });
    }

    @Test
    @MongoDBFixtures("legacy-alert-conditions.json")
    public void runWithMigrationStatus() {
        final int migratedConditions = 9; // Only 8 because we pass one migrated condition in
        final int migratedCallbacks = 3;  // Only 2 because we pass one migrated callback in

        assertThat(migrator.run(Collections.singleton("00000000-0000-0000-0000-000000000002"), Collections.singleton("54e3deadbeefdeadbeef0001"))).satisfies(result -> {
            assertThat(result.completedAlertConditions()).containsOnly(
                    "00000000-0000-0000-0000-000000000001",
                    "00000000-0000-0000-0000-000000000002",
                    "00000000-0000-0000-0000-000000000003",
                    "00000000-0000-0000-0000-000000000004",
                    "00000000-0000-0000-0000-000000000005",
                    "00000000-0000-0000-0000-000000000006",
                    "00000000-0000-0000-0000-000000000007",
                    "00000000-0000-0000-0000-000000000008",
                    "00000000-0000-0000-0000-000000000009",
                    "00000000-0000-0000-0000-000000000010"
            );
            assertThat(result.completedAlarmCallbacks()).containsOnly(
                    "54e3deadbeefdeadbeef0001",
                    "54e3deadbeefdeadbeef0002",
                    "54e3deadbeefdeadbeef0003",
                    "54e3deadbeefdeadbeef0004"
            );
        });

        // Make sure we use the EventDefinitionHandler to create the event definitions
        verify(eventDefinitionHandler, times(migratedConditions)).create(any(EventDefinitionDto.class), any(Optional.class));

        // Make sure we use the NotificationResourceHandler to create the notifications
        verify(notificationResourceHandler, times(migratedCallbacks)).create(any(NotificationDto.class), any(Optional.class));

        assertThat(eventDefinitionService.streamAll().count()).isEqualTo(migratedConditions);
        assertThat(notificationService.streamAll().count()).isEqualTo(migratedCallbacks);
    }
}
