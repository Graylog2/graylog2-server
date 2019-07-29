/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.events;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.events.audit.EventsAuditEventTypes;
import org.graylog.events.fields.EventFieldSpecEngine;
import org.graylog.events.fields.providers.LookupTableFieldValueProvider;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.indices.MoreIndices;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotification;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.legacy.V20190722150700_LegacyAlertConditionMigration;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.events.notifications.NotificationGracePeriodService;
import org.graylog.events.notifications.types.EmailEventNotification;
import org.graylog.events.notifications.types.EmailEventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotification;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.periodicals.EventNotificationStatusCleanUp;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.aggregation.AggregationEventProcessor;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.aggregation.AggregationEventProcessorParameters;
import org.graylog.events.processor.aggregation.AggregationSearch;
import org.graylog.events.processor.aggregation.PivotAggregationSearch;
import org.graylog.events.processor.storage.EventStorageHandlerEngine;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.scheduler.JobExecutionEngine;
import org.graylog.scheduler.JobTriggerUpdates;
import org.graylog.events.processor.EventProcessorExecutionJob;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.scheduler.worker.JobWorkerPool;
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
        bind(MoreIndices.class).asEagerSingleton();
        bind(NotificationGracePeriodService.class).asEagerSingleton();

        install(new FactoryModuleBuilder().build(JobExecutionEngine.Factory.class));
        install(new FactoryModuleBuilder().build(JobWorkerPool.Factory.class));
        install(new FactoryModuleBuilder().build(JobTriggerUpdates.Factory.class));

        // Add all rest resources in this package
        registerRestControllerPackage(getClass().getPackage().getName());

        addPeriodical(EventNotificationStatusCleanUp.class);

        addMigration(V20190722150700_LegacyAlertConditionMigration.class);
        addAuditEventTypes(EventsAuditEventTypes.class);

        addEventProcessor(AggregationEventProcessorConfig.TYPE_NAME,
                AggregationEventProcessor.class,
                AggregationEventProcessor.Factory.class,
                AggregationEventProcessorConfig.class,
                AggregationEventProcessorParameters.class);

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
                EmailEventNotification.Factory.class);
        addNotificationType(HTTPEventNotificationConfig.TYPE_NAME,
                HTTPEventNotificationConfig.class,
                HTTPEventNotification.class,
                HTTPEventNotification.Factory.class);
        addNotificationType(LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME,
                LegacyAlarmCallbackEventNotificationConfig.class,
                LegacyAlarmCallbackEventNotification.class,
                LegacyAlarmCallbackEventNotification.Factory.class);

        addJobSchedulerSchedule(IntervalJobSchedule.TYPE_NAME, IntervalJobSchedule.class);
        addJobSchedulerSchedule(OnceJobSchedule.TYPE_NAME, OnceJobSchedule.class);

        // Change this if another aggregation search implementation should be used
        install(new FactoryModuleBuilder().implement(AggregationSearch.class, PivotAggregationSearch.class).build(AggregationSearch.Factory.class));
    }
}
