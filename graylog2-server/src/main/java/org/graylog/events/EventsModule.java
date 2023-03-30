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
package org.graylog.events;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.contentpack.entities.AggregationEventProcessorConfigEntity;
import org.graylog.events.contentpack.entities.EmailEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.LegacyAlarmCallbackEventNotificationConfigEntity;
import org.graylog.events.contentpack.facade.EventDefinitionFacade;
import org.graylog.events.contentpack.facade.NotificationFacade;
import org.graylog.events.fields.EventFieldSpecEngine;
import org.graylog.events.fields.providers.LookupTableFieldValueProvider;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.indices.EventIndexer;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotification;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.events.notifications.EventNotificationExecutionMetrics;
import org.graylog.events.notifications.NotificationGracePeriodService;
import org.graylog.events.notifications.types.EmailEventNotification;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotification;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.periodicals.EventNotificationStatusCleanUp;
import org.graylog.events.processor.DefaultEventResolver;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.events.processor.EventProcessorExecutionMetrics;
import org.graylog.events.processor.EventResolver;
import org.graylog.events.processor.aggregation.AggregationEventProcessor;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationEventProcessorParameters;
import org.graylog.events.processor.aggregation.AggregationSearch;
import org.graylog.events.processor.aggregation.PivotAggregationSearch;
import org.graylog.events.processor.storage.EventStorageHandlerEngine;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.events.processor.systemnotification.SystemNotificationEventEntityScope;
import org.graylog.events.processor.systemnotification.SystemNotificationEventProcessor;
import org.graylog.events.processor.systemnotification.SystemNotificationEventProcessorConfig;
import org.graylog.events.processor.systemnotification.SystemNotificationEventProcessorParameters;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderResource;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.events.rest.AvailableEntityTypesResource;
import org.graylog.events.rest.EventDefinitionsResource;
import org.graylog.events.rest.EventNotificationsResource;
import org.graylog.events.rest.EventsResource;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class EventsModule extends PluginModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {
        bind(EventProcessorEngine.class).asEagerSingleton();
        bind(EventStorageHandlerEngine.class).asEagerSingleton();
        bind(EventFieldSpecEngine.class).asEagerSingleton();
        bind(EventIndexer.class).asEagerSingleton();
        bind(NotificationGracePeriodService.class).asEagerSingleton();
        bind(EventProcessorExecutionMetrics.class).asEagerSingleton();
        bind(EventNotificationExecutionMetrics.class).asEagerSingleton();
        bind(SystemNotificationRenderService.class).asEagerSingleton();

        OptionalBinder.newOptionalBinder(binder(), EventResolver.class)
                .setDefault().to(DefaultEventResolver.class);

        addEntityScope(SystemNotificationEventEntityScope.class);

        addSystemRestResource(AvailableEntityTypesResource.class);
        addSystemRestResource(EventDefinitionsResource.class);
        addSystemRestResource(EventNotificationsResource.class);
        addSystemRestResource(EventsResource.class);
        addSystemRestResource(SystemNotificationRenderResource.class);

        addPeriodical(EventNotificationStatusCleanUp.class);

        addEntityFacade(ModelTypes.EVENT_DEFINITION_V1, EventDefinitionFacade.class);
        addEntityFacade(ModelTypes.NOTIFICATION_V1, NotificationFacade.class);

        addMigration(V20190722150700_LegacyAlertConditionMigration.class);
        addAuditEventTypes(EventsAuditEventTypes.class);

        registerJacksonSubtype(AggregationEventProcessorConfigEntity.class,
                AggregationEventProcessorConfigEntity.TYPE_NAME);

        addEventProcessor(AggregationEventProcessorConfig.TYPE_NAME,
                AggregationEventProcessor.class,
                AggregationEventProcessor.Factory.class,
                AggregationEventProcessorConfig.class,
                AggregationEventProcessorParameters.class);
        addEventProcessor(SystemNotificationEventProcessorConfig.TYPE_NAME,
                SystemNotificationEventProcessor.class,
                SystemNotificationEventProcessor.Factory.class,
                SystemNotificationEventProcessorConfig.class,
                SystemNotificationEventProcessorParameters.class);

        addEventStorageHandler(PersistToStreamsStorageHandler.Config.TYPE_NAME,
                PersistToStreamsStorageHandler.class,
                PersistToStreamsStorageHandler.Factory.class,
                PersistToStreamsStorageHandler.Config.class);

        addEventFieldValueProvider(TemplateFieldValueProvider.Config.TYPE_NAME,
                TemplateFieldValueProvider.class,
                TemplateFieldValueProvider.Factory.class,
                TemplateFieldValueProvider.Config.class);
        addEventFieldValueProvider(LookupTableFieldValueProvider.Config.TYPE_NAME,
                LookupTableFieldValueProvider.class,
                LookupTableFieldValueProvider.Factory.class,
                LookupTableFieldValueProvider.Config.class);

        addSchedulerJob(EventProcessorExecutionJob.TYPE_NAME,
                EventProcessorExecutionJob.class,
                EventProcessorExecutionJob.Factory.class,
                EventProcessorExecutionJob.Config.class,
                EventProcessorExecutionJob.Data.class);
        addSchedulerJob(EventNotificationExecutionJob.TYPE_NAME,
                EventNotificationExecutionJob.class,
                EventNotificationExecutionJob.Factory.class,
                EventNotificationExecutionJob.Config.class,
                EventNotificationExecutionJob.Data.class);

        addNotificationType(EmailEventNotificationConfig.TYPE_NAME,
                EmailEventNotificationConfig.class,
                EmailEventNotification.class,
                EmailEventNotification.Factory.class,
                EmailEventNotificationConfigEntity.TYPE_NAME,
                EmailEventNotificationConfigEntity.class);
        addNotificationType(HTTPEventNotificationConfig.TYPE_NAME,
                HTTPEventNotificationConfig.class,
                HTTPEventNotification.class,
                HTTPEventNotification.Factory.class,
                HttpEventNotificationConfigEntity.TYPE_NAME,
                HttpEventNotificationConfigEntity.class);
        addNotificationType(LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME,
                LegacyAlarmCallbackEventNotificationConfig.class,
                LegacyAlarmCallbackEventNotification.class,
                LegacyAlarmCallbackEventNotification.Factory.class,
                LegacyAlarmCallbackEventNotificationConfigEntity.TYPE_NAME,
                LegacyAlarmCallbackEventNotificationConfigEntity.class);

        addJobSchedulerSchedule(IntervalJobSchedule.TYPE_NAME, IntervalJobSchedule.class);
        addJobSchedulerSchedule(OnceJobSchedule.TYPE_NAME, OnceJobSchedule.class);

        // Change this if another aggregation search implementation should be used
        install(new FactoryModuleBuilder().implement(AggregationSearch.class, PivotAggregationSearch.class).build(AggregationSearch.Factory.class));
    }
}
