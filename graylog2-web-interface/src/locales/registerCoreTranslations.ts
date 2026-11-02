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
// Only imported by the core entry point, so plugins, which compile core modules into their bundles, don't contain
// a copy of the core translations.
import { registerTranslations } from 'i18n';
import enAggregation from 'locales/en/aggregation.json';
import enAuthentication from 'locales/en/authentication.json';
import enClusterCertificateManagement from 'locales/en/cluster_certificate_management.json';
import enCommon from 'locales/en/common.json';
import enContentPacks from 'locales/en/content_packs.json';
import enDashboards from 'locales/en/dashboards.json';
import enDataNodesClusterManagement from 'locales/en/data_nodes_cluster_management.json';
import enEventNotifications from 'locales/en/event_notifications.json';
import enEvents from 'locales/en/events.json';
import enExtractors from 'locales/en/extractors.json';
import enInputs from 'locales/en/inputs.json';
import enIntegrations from 'locales/en/integrations.json';
import enLogin from 'locales/en/login.json';
import enNavigation from 'locales/en/navigation.json';
import enPages from 'locales/en/pages.json';
import enSearch from 'locales/en/search.json';
import enSidecars from 'locales/en/sidecars.json';
import enStreams from 'locales/en/streams.json';
import enWelcome from 'locales/en/welcome.json';
import enWidgets from 'locales/en/widgets.json';
import enCollectors from 'locales/en/collectors.json';
import enEventDefinitions from 'locales/en/event_definitions.json';
import enGrokpatterns from 'locales/en/grokpatterns.json';
import enIndexsets from 'locales/en/indexsets.json';
import enLoggers from 'locales/en/loggers.json';
import enLookup_tables from 'locales/en/lookup_tables.json';
import enPipelines from 'locales/en/pipelines.json';
import enUsers from 'locales/en/users.json';
import enDataNode from 'locales/en/datanode.json';
import enApiBrowser from 'locales/en/api_browser.json';
import enCertificates from 'locales/en/certificates.json';
import enMessageloaders from 'locales/en/messageloaders.json';
import enSystem from 'locales/en/system.json';
import enConfigurations from 'locales/en/configurations.json';
import 'locales/registerPreflightTranslations';

registerTranslations('aggregation', enAggregation, {
  de: () => import(/* webpackChunkName: "translations-de-aggregation" */ 'locales/de/aggregation.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-aggregation" */ 'locales/pl/aggregation.json'),
});

registerTranslations('authentication', enAuthentication, {
  de: () => import(/* webpackChunkName: "translations-de-authentication" */ 'locales/de/authentication.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-authentication" */ 'locales/pl/authentication.json'),
});

registerTranslations('cluster_certificate_management', enClusterCertificateManagement, {
  de: () =>
    import(
      /* webpackChunkName: "translations-de-cluster-certificate-management" */ 'locales/de/cluster_certificate_management.json'
    ),
  pl: () =>
    import(
      /* webpackChunkName: "translations-pl-cluster-certificate-management" */ 'locales/pl/cluster_certificate_management.json'
    ),
});

registerTranslations('common', enCommon, {
  de: () => import(/* webpackChunkName: "translations-de-common" */ 'locales/de/common.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-common" */ 'locales/pl/common.json'),
});

registerTranslations('content_packs', enContentPacks, {
  de: () => import(/* webpackChunkName: "translations-de-content-packs" */ 'locales/de/content_packs.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-content-packs" */ 'locales/pl/content_packs.json'),
});

registerTranslations('dashboards', enDashboards, {
  de: () => import(/* webpackChunkName: "translations-de-dashboards" */ 'locales/de/dashboards.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-dashboards" */ 'locales/pl/dashboards.json'),
});

registerTranslations('data_nodes_cluster_management', enDataNodesClusterManagement, {
  de: () =>
    import(
      /* webpackChunkName: "translations-de-data-nodes-cluster-management" */ 'locales/de/data_nodes_cluster_management.json'
    ),
  pl: () =>
    import(
      /* webpackChunkName: "translations-pl-data-nodes-cluster-management" */ 'locales/pl/data_nodes_cluster_management.json'
    ),
});

registerTranslations('event_notifications', enEventNotifications, {
  de: () => import(/* webpackChunkName: "translations-de-event-notifications" */ 'locales/de/event_notifications.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-event-notifications" */ 'locales/pl/event_notifications.json'),
});

registerTranslations('events', enEvents, {
  de: () => import(/* webpackChunkName: "translations-de-events" */ 'locales/de/events.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-events" */ 'locales/pl/events.json'),
});

registerTranslations('extractors', enExtractors, {
  de: () => import(/* webpackChunkName: "translations-de-extractors" */ 'locales/de/extractors.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-extractors" */ 'locales/pl/extractors.json'),
});

registerTranslations('inputs', enInputs, {
  de: () => import(/* webpackChunkName: "translations-de-inputs" */ 'locales/de/inputs.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-inputs" */ 'locales/pl/inputs.json'),
});

registerTranslations('integrations', enIntegrations, {
  de: () => import(/* webpackChunkName: "translations-de-integrations" */ 'locales/de/integrations.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-integrations" */ 'locales/pl/integrations.json'),
});

registerTranslations('login', enLogin, {
  de: () => import(/* webpackChunkName: "translations-de-login" */ 'locales/de/login.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-login" */ 'locales/pl/login.json'),
});

registerTranslations('navigation', enNavigation, {
  de: () => import(/* webpackChunkName: "translations-de-navigation" */ 'locales/de/navigation.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-navigation" */ 'locales/pl/navigation.json'),
});

registerTranslations('pages', enPages, {
  de: () => import(/* webpackChunkName: "translations-de-pages" */ 'locales/de/pages.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-pages" */ 'locales/pl/pages.json'),
});

registerTranslations('search', enSearch, {
  de: () => import(/* webpackChunkName: "translations-de-search" */ 'locales/de/search.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-search" */ 'locales/pl/search.json'),
});

registerTranslations('sidecars', enSidecars, {
  de: () => import(/* webpackChunkName: "translations-de-sidecars" */ 'locales/de/sidecars.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-sidecars" */ 'locales/pl/sidecars.json'),
});

registerTranslations('streams', enStreams, {
  de: () => import(/* webpackChunkName: "translations-de-streams" */ 'locales/de/streams.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-streams" */ 'locales/pl/streams.json'),
});

registerTranslations('welcome', enWelcome, {
  de: () => import(/* webpackChunkName: "translations-de-welcome" */ 'locales/de/welcome.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-welcome" */ 'locales/pl/welcome.json'),
});

registerTranslations('widgets', enWidgets, {
  de: () => import(/* webpackChunkName: "translations-de-widgets" */ 'locales/de/widgets.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-widgets" */ 'locales/pl/widgets.json'),
});

registerTranslations('collectors', enCollectors, {
  de: () => import(/* webpackChunkName: "translations-de-collectors" */ 'locales/de/collectors.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-collectors" */ 'locales/pl/collectors.json'),
});

registerTranslations('event_definitions', enEventDefinitions, {
  de: () => import(/* webpackChunkName: "translations-de-event_definitions" */ 'locales/de/event_definitions.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-event_definitions" */ 'locales/pl/event_definitions.json'),
});

registerTranslations('grokpatterns', enGrokpatterns, {
  de: () => import(/* webpackChunkName: "translations-de-grokpatterns" */ 'locales/de/grokpatterns.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-grokpatterns" */ 'locales/pl/grokpatterns.json'),
});

registerTranslations('indexsets', enIndexsets, {
  de: () => import(/* webpackChunkName: "translations-de-indexsets" */ 'locales/de/indexsets.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-indexsets" */ 'locales/pl/indexsets.json'),
});

registerTranslations('loggers', enLoggers, {
  de: () => import(/* webpackChunkName: "translations-de-loggers" */ 'locales/de/loggers.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-loggers" */ 'locales/pl/loggers.json'),
});

registerTranslations('lookup_tables', enLookup_tables, {
  de: () => import(/* webpackChunkName: "translations-de-lookup_tables" */ 'locales/de/lookup_tables.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-lookup_tables" */ 'locales/pl/lookup_tables.json'),
});

registerTranslations('pipelines', enPipelines, {
  de: () => import(/* webpackChunkName: "translations-de-pipelines" */ 'locales/de/pipelines.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-pipelines" */ 'locales/pl/pipelines.json'),
});

registerTranslations('users', enUsers, {
  de: () => import(/* webpackChunkName: "translations-de-users" */ 'locales/de/users.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-users" */ 'locales/pl/users.json'),
});

registerTranslations('datanode', enDataNode, {
  de: () => import(/* webpackChunkName: "translations-de-datanode" */ 'locales/de/datanode.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-datanode" */ 'locales/pl/datanode.json'),
});

registerTranslations('api_browser', enApiBrowser, {
  de: () => import(/* webpackChunkName: "translations-de-api_browser" */ 'locales/de/api_browser.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-api_browser" */ 'locales/pl/api_browser.json'),
});

registerTranslations('certificates', enCertificates, {
  de: () => import(/* webpackChunkName: "translations-de-certificates" */ 'locales/de/certificates.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-certificates" */ 'locales/pl/certificates.json'),
});

registerTranslations('messageloaders', enMessageloaders, {
  de: () => import(/* webpackChunkName: "translations-de-messageloaders" */ 'locales/de/messageloaders.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-messageloaders" */ 'locales/pl/messageloaders.json'),
});

registerTranslations('system', enSystem, {
  de: () => import(/* webpackChunkName: "translations-de-system" */ 'locales/de/system.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-system" */ 'locales/pl/system.json'),
});

registerTranslations('configurations', enConfigurations, {
  de: () => import(/* webpackChunkName: "translations-de-configurations" */ 'locales/de/configurations.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-configurations" */ 'locales/pl/configurations.json'),
});
