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
import enCollectorsDeployment from 'locales/en/collectors_deployment.json';
import enCollectorsFleets from 'locales/en/collectors_fleets.json';
import enCollectorsInstances from 'locales/en/collectors_instances.json';
import enCollectorsOnboarding from 'locales/en/collectors_onboarding.json';
import enCollectorsOverview from 'locales/en/collectors_overview.json';
import enCollectorsSettings from 'locales/en/collectors_settings.json';
import enCommon from 'locales/en/common.json';
import enContentPacks from 'locales/en/content_packs.json';
import enDashboards from 'locales/en/dashboards.json';
import enDataNodesClusterManagement from 'locales/en/data_nodes_cluster_management.json';
import enEventNotifications from 'locales/en/event_notifications.json';
import enEvents from 'locales/en/events.json';
import enExtractors from 'locales/en/extractors.json';
import enInputs from 'locales/en/inputs.json';
import enLogin from 'locales/en/login.json';
import enNavigation from 'locales/en/navigation.json';
import enSearch from 'locales/en/search.json';
import enStreams from 'locales/en/streams.json';
import enWelcome from 'locales/en/welcome.json';
import enWidgets from 'locales/en/widgets.json';

registerTranslations('aggregation', enAggregation, {
  de: () => import(/* webpackChunkName: "translations-de-aggregation" */ 'locales/de/aggregation.json'),
});

registerTranslations('authentication', enAuthentication, {
  de: () => import(/* webpackChunkName: "translations-de-authentication" */ 'locales/de/authentication.json'),
});

registerTranslations('cluster_certificate_management', enClusterCertificateManagement, {
  de: () => import(/* webpackChunkName: "translations-de-cluster-certificate-management" */ 'locales/de/cluster_certificate_management.json'),
});

registerTranslations('collectors_deployment', enCollectorsDeployment, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-deployment" */ 'locales/de/collectors_deployment.json'),
});

registerTranslations('collectors_fleets', enCollectorsFleets, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-fleets" */ 'locales/de/collectors_fleets.json'),
});

registerTranslations('collectors_instances', enCollectorsInstances, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-instances" */ 'locales/de/collectors_instances.json'),
});

registerTranslations('collectors_onboarding', enCollectorsOnboarding, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-onboarding" */ 'locales/de/collectors_onboarding.json'),
});

registerTranslations('collectors_overview', enCollectorsOverview, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-overview" */ 'locales/de/collectors_overview.json'),
});

registerTranslations('collectors_settings', enCollectorsSettings, {
  de: () => import(/* webpackChunkName: "translations-de-collectors-settings" */ 'locales/de/collectors_settings.json'),
});

registerTranslations('common', enCommon, {
  de: () => import(/* webpackChunkName: "translations-de-common" */ 'locales/de/common.json'),
});

registerTranslations('content_packs', enContentPacks, {
  de: () => import(/* webpackChunkName: "translations-de-content-packs" */ 'locales/de/content_packs.json'),
});

registerTranslations('dashboards', enDashboards, {
  de: () => import(/* webpackChunkName: "translations-de-dashboards" */ 'locales/de/dashboards.json'),
});

registerTranslations('data_nodes_cluster_management', enDataNodesClusterManagement, {
  de: () => import(/* webpackChunkName: "translations-de-data-nodes-cluster-management" */ 'locales/de/data_nodes_cluster_management.json'),
});

registerTranslations('event_notifications', enEventNotifications, {
  de: () => import(/* webpackChunkName: "translations-de-event-notifications" */ 'locales/de/event_notifications.json'),
});

registerTranslations('events', enEvents, {
  de: () => import(/* webpackChunkName: "translations-de-events" */ 'locales/de/events.json'),
});

registerTranslations('extractors', enExtractors, {
  de: () => import(/* webpackChunkName: "translations-de-extractors" */ 'locales/de/extractors.json'),
});

registerTranslations('inputs', enInputs, {
  de: () => import(/* webpackChunkName: "translations-de-inputs" */ 'locales/de/inputs.json'),
});

registerTranslations('login', enLogin, {
  de: () => import(/* webpackChunkName: "translations-de-login" */ 'locales/de/login.json'),
});

registerTranslations('navigation', enNavigation, {
  de: () => import(/* webpackChunkName: "translations-de-navigation" */ 'locales/de/navigation.json'),
});

registerTranslations('search', enSearch, {
  de: () => import(/* webpackChunkName: "translations-de-search" */ 'locales/de/search.json'),
});

registerTranslations('streams', enStreams, {
  de: () => import(/* webpackChunkName: "translations-de-streams" */ 'locales/de/streams.json'),
});

registerTranslations('welcome', enWelcome, {
  de: () => import(/* webpackChunkName: "translations-de-welcome" */ 'locales/de/welcome.json'),
});

registerTranslations('widgets', enWidgets, {
  de: () => import(/* webpackChunkName: "translations-de-widgets" */ 'locales/de/widgets.json'),
});
