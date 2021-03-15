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
package org.graylog2.rest.resources;

import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.rest.resources.alarmcallbacks.AlarmCallbackHistoryResource;
import org.graylog2.rest.resources.alarmcallbacks.AlarmCallbacksResource;
import org.graylog2.rest.resources.alarmcallbacks.StreamAlarmCallbackResource;
import org.graylog2.rest.resources.alerts.AlertResource;
import org.graylog2.rest.resources.cluster.ClusterDeflectorResource;
import org.graylog2.rest.resources.cluster.ClusterInputStatesResource;
import org.graylog2.rest.resources.cluster.ClusterJournalResource;
import org.graylog2.rest.resources.cluster.ClusterLoadBalancerStatusResource;
import org.graylog2.rest.resources.cluster.ClusterLoggersResource;
import org.graylog2.rest.resources.cluster.ClusterMetricsResource;
import org.graylog2.rest.resources.cluster.ClusterNodeMetricsResource;
import org.graylog2.rest.resources.cluster.ClusterSystemJobResource;
import org.graylog2.rest.resources.cluster.ClusterSystemPluginResource;
import org.graylog2.rest.resources.cluster.ClusterSystemProcessingResource;
import org.graylog2.rest.resources.cluster.ClusterSystemResource;
import org.graylog2.rest.resources.cluster.ClusterSystemShutdownResource;
import org.graylog2.rest.resources.messages.MessageResource;
import org.graylog2.rest.resources.roles.RolesResource;
import org.graylog2.rest.resources.search.AbsoluteSearchResource;
import org.graylog2.rest.resources.search.DecoratorResource;
import org.graylog2.rest.resources.search.KeywordSearchResource;
import org.graylog2.rest.resources.search.RelativeSearchResource;
import org.graylog2.rest.resources.streams.StreamResource;
import org.graylog2.rest.resources.streams.alerts.AlertConditionsResource;
import org.graylog2.rest.resources.streams.alerts.StreamAlertConditionResource;
import org.graylog2.rest.resources.streams.alerts.StreamAlertResource;
import org.graylog2.rest.resources.streams.outputs.StreamOutputResource;
import org.graylog2.rest.resources.streams.rules.StreamRuleResource;
import org.graylog2.rest.resources.system.ClusterConfigResource;
import org.graylog2.rest.resources.system.ClusterResource;
import org.graylog2.rest.resources.system.ClusterStatsResource;
import org.graylog2.rest.resources.system.ConfigurationResource;
import org.graylog2.rest.resources.system.DeflectorResource;
import org.graylog2.rest.resources.system.GettingStartedResource;
import org.graylog2.rest.resources.system.GrokResource;
import org.graylog2.rest.resources.system.IndexRangesResource;
import org.graylog2.rest.resources.system.JournalResource;
import org.graylog2.rest.resources.system.MessageProcessorsResource;
import org.graylog2.rest.resources.system.MessagesResource;
import org.graylog2.rest.resources.system.MetricsHistoryResource;
import org.graylog2.rest.resources.system.NotificationsResource;
import org.graylog2.rest.resources.system.PermissionsResource;
import org.graylog2.rest.resources.system.SessionsResource;
import org.graylog2.rest.resources.system.StatsResource;
import org.graylog2.rest.resources.system.SystemFieldsResource;
import org.graylog2.rest.resources.system.SystemProcessingResource;
import org.graylog2.rest.resources.system.SystemShutdownResource;
import org.graylog2.rest.resources.system.TrafficResource;
import org.graylog2.rest.resources.system.UrlWhitelistResource;
import org.graylog2.rest.resources.system.contentpacks.CatalogResource;
import org.graylog2.rest.resources.system.contentpacks.ContentPackResource;
import org.graylog2.rest.resources.system.debug.DebugEventsResource;
import org.graylog2.rest.resources.system.indexer.FailuresResource;
import org.graylog2.rest.resources.system.indexer.IndexSetsResource;
import org.graylog2.rest.resources.system.indexer.IndexTemplatesResource;
import org.graylog2.rest.resources.system.indexer.IndexerClusterResource;
import org.graylog2.rest.resources.system.indexer.IndexerOverviewResource;
import org.graylog2.rest.resources.system.indexer.IndicesResource;
import org.graylog2.rest.resources.system.indices.RetentionStrategyResource;
import org.graylog2.rest.resources.system.indices.RotationStrategyResource;
import org.graylog2.rest.resources.system.inputs.ExtractorsResource;
import org.graylog2.rest.resources.system.inputs.InputStatesResource;
import org.graylog2.rest.resources.system.inputs.InputsResource;
import org.graylog2.rest.resources.system.inputs.StaticFieldsResource;
import org.graylog2.rest.resources.system.jobs.ServiceManagerResource;
import org.graylog2.rest.resources.system.jobs.SystemJobResource;
import org.graylog2.rest.resources.system.logs.LoggersResource;
import org.graylog2.rest.resources.system.lookup.LookupTableResource;
import org.graylog2.rest.resources.system.outputs.OutputResource;
import org.graylog2.rest.resources.system.processing.ClusterProcessingStatusResource;
import org.graylog2.rest.resources.system.processing.SystemProcessingStatusResource;
import org.graylog2.rest.resources.tools.ContainsStringTesterResource;
import org.graylog2.rest.resources.tools.GrokTesterResource;
import org.graylog2.rest.resources.tools.JsonTesterResource;
import org.graylog2.rest.resources.tools.LookupTableTesterResource;
import org.graylog2.rest.resources.tools.NaturalDateTesterResource;
import org.graylog2.rest.resources.tools.RegexReplaceTesterResource;
import org.graylog2.rest.resources.tools.RegexTesterResource;
import org.graylog2.rest.resources.tools.SplitAndIndexTesterResource;
import org.graylog2.rest.resources.tools.SubstringTesterResource;
import org.graylog2.rest.resources.users.UsersResource;

public class RestResourcesModule extends Graylog2Module {
    @Override
    protected void configure() {
        addAuthResources();
        addLegacyAlertingResources();
        addClusterResources();
        addContentPackResources();
        addIndexingResources();
        addLegacySearchResources();
        addProcessingResources();
        addStreamsResources();

        addSystemRestResource(HelloWorldResource.class);
        addSystemRestResource(RolesResource.class);
        addSystemRestResource(ClusterConfigResource.class);
        addSystemRestResource(ClusterResource.class);
        addSystemRestResource(ClusterStatsResource.class);
        addSystemRestResource(ConfigurationResource.class);
        addSystemRestResource(DebugEventsResource.class);
        addSystemRestResource(GettingStartedResource.class);
        addSystemRestResource(ServiceManagerResource.class);
        addSystemRestResource(SystemJobResource.class);
        addSystemRestResource(JournalResource.class);
        addSystemRestResource(LoggersResource.class);
        addSystemRestResource(MessagesResource.class);
        addSystemRestResource(MetricsHistoryResource.class);
        addSystemRestResource(NotificationsResource.class);
        addSystemRestResource(StatsResource.class);
        addSystemRestResource(SystemShutdownResource.class);
        addSystemRestResource(TrafficResource.class);
    }

    private void addAuthResources() {
        addSystemRestResource(PermissionsResource.class);
        addSystemRestResource(SessionsResource.class);
        addSystemRestResource(UsersResource.class);
    }

    private void addLegacyAlertingResources() {
        addSystemRestResource(AlarmCallbackHistoryResource.class);
        addSystemRestResource(AlarmCallbacksResource.class);
        addSystemRestResource(StreamAlarmCallbackResource.class);
        addSystemRestResource(AlertResource.class);
        addSystemRestResource(AlertConditionsResource.class);
        addSystemRestResource(StreamAlertConditionResource.class);
        addSystemRestResource(StreamAlertResource.class);
    }

    private void addClusterResources() {
        addSystemRestResource(ClusterDeflectorResource.class);
        addSystemRestResource(ClusterInputStatesResource.class);
        addSystemRestResource(ClusterJournalResource.class);
        addSystemRestResource(ClusterLoadBalancerStatusResource.class);
        addSystemRestResource(ClusterLoggersResource.class);
        addSystemRestResource(ClusterMetricsResource.class);
        addSystemRestResource(ClusterNodeMetricsResource.class);
        addSystemRestResource(ClusterSystemJobResource.class);
        addSystemRestResource(ClusterSystemPluginResource.class);
        addSystemRestResource(ClusterSystemProcessingResource.class);
        addSystemRestResource(ClusterSystemResource.class);
        addSystemRestResource(ClusterSystemShutdownResource.class);
    }

    private void addContentPackResources() {
        addSystemRestResource(CatalogResource.class);
        addSystemRestResource(ContentPackResource.class);
    }

    private void addIndexingResources() {
        addSystemRestResource(DeflectorResource.class);
        addSystemRestResource(FailuresResource.class);
        addSystemRestResource(IndexerClusterResource.class);
        addSystemRestResource(IndexerOverviewResource.class);
        addSystemRestResource(IndexSetsResource.class);
        addSystemRestResource(IndexTemplatesResource.class);
        addSystemRestResource(IndicesResource.class);
        addSystemRestResource(IndexRangesResource.class);
        addSystemRestResource(RetentionStrategyResource.class);
        addSystemRestResource(RotationStrategyResource.class);
    }

    private void addLegacySearchResources() {
        addSystemRestResource(AbsoluteSearchResource.class);
        addSystemRestResource(DecoratorResource.class);
        addSystemRestResource(KeywordSearchResource.class);
        addSystemRestResource(RelativeSearchResource.class);
        addSystemRestResource(MessageResource.class);
        addSystemRestResource(SystemFieldsResource.class);
    }

    private void addProcessingResources() {
        addSystemRestResource(GrokResource.class);
        addSystemRestResource(ExtractorsResource.class);
        addSystemRestResource(InputsResource.class);
        addSystemRestResource(InputStatesResource.class);
        addSystemRestResource(StaticFieldsResource.class);
        addSystemRestResource(LookupTableResource.class);
        addSystemRestResource(MessageProcessorsResource.class);
        addSystemRestResource(OutputResource.class);
        addSystemRestResource(SystemProcessingResource.class);
        addSystemRestResource(ClusterProcessingStatusResource.class);
        addSystemRestResource(SystemProcessingStatusResource.class);
        addSystemRestResource(UrlWhitelistResource.class);
        addSystemRestResource(ContainsStringTesterResource.class);
        addSystemRestResource(GrokTesterResource.class);
        addSystemRestResource(JsonTesterResource.class);
        addSystemRestResource(LookupTableTesterResource.class);
        addSystemRestResource(NaturalDateTesterResource.class);
        addSystemRestResource(RegexReplaceTesterResource.class);
        addSystemRestResource(RegexTesterResource.class);
        addSystemRestResource(SplitAndIndexTesterResource.class);
        addSystemRestResource(SubstringTesterResource.class);
    }

    private void addStreamsResources() {
        addSystemRestResource(StreamOutputResource.class);
        addSystemRestResource(StreamRuleResource.class);
        addSystemRestResource(StreamResource.class);
    }
}
