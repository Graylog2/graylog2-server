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
import enApiBrowser from 'locales/en/api_browser.json';
import enAuthentication from 'locales/en/authentication.json';
import enBuiltinRoles from 'locales/en/builtin_roles.json';
import enCertificates from 'locales/en/certificates.json';
import enClusterCertificateManagement from 'locales/en/cluster_certificate_management.json';
import enCollectors from 'locales/en/collectors.json';
import enCommon from 'locales/en/common.json';
import enConfigurations from 'locales/en/configurations.json';
import enContentPacks from 'locales/en/content_packs.json';
import enDashboards from 'locales/en/dashboards.json';
import enDataNodesClusterManagement from 'locales/en/data_nodes_cluster_management.json';
import enDatanode from 'locales/en/datanode.json';
import enEntityAttributes from 'locales/en/entity_attributes.json';
import enEventDefinitions from 'locales/en/event_definitions.json';
import enEventNotifications from 'locales/en/event_notifications.json';
import enEvents from 'locales/en/events.json';
import enExtractors from 'locales/en/extractors.json';
import enGrokpatterns from 'locales/en/grokpatterns.json';
import enIndexSetTemplates from 'locales/en/index_set_templates.json';
import enIndexsets from 'locales/en/indexsets.json';
import enInputs from 'locales/en/inputs.json';
import enIntegrations from 'locales/en/integrations.json';
import enLoggers from 'locales/en/loggers.json';
import enLogin from 'locales/en/login.json';
import enLookupTables from 'locales/en/lookup_tables.json';
import enMessageloaders from 'locales/en/messageloaders.json';
import enNavigation from 'locales/en/navigation.json';
import enPages from 'locales/en/pages.json';
import enPipelines from 'locales/en/pipelines.json';
import enPreflight from 'locales/en/preflight.json';
import enSearch from 'locales/en/search.json';
import enServerErrors from 'locales/en/server_errors.json';
import enServerFields from 'locales/en/server_fields.json';
import enSidecars from 'locales/en/sidecars.json';
import enStreamRuleTypes from 'locales/en/stream_rule_types.json';
import enStreamRuleTypesLong from 'locales/en/stream_rule_types_long.json';
import enStreams from 'locales/en/streams.json';
import enSystem from 'locales/en/system.json';
import enTranslationFeedback from 'locales/en/translation_feedback.json';
import enUsers from 'locales/en/users.json';
import enWelcome from 'locales/en/welcome.json';
import enWidgets from 'locales/en/widgets.json';

registerTranslations('aggregation', enAggregation, {
  de: () => import(/* webpackChunkName: "translations-de-aggregation" */ 'locales/de/aggregation.json'),
  it: () => import(/* webpackChunkName: "translations-it-aggregation" */ 'locales/it/aggregation.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-aggregation" */ 'locales/zh-TW/aggregation.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-aggregation" */ 'locales/ja/aggregation.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-aggregation" */ 'locales/pl/aggregation.json'),
  es: () => import(/* webpackChunkName: "translations-es-aggregation" */ 'locales/es/aggregation.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-aggregation" */ 'locales/fr/aggregation.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-aggregation" */ 'locales/pt-BR/aggregation.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-aggregation" */ 'locales/zh-CN/aggregation.json'),
});

registerTranslations('api_browser', enApiBrowser, {
  de: () => import(/* webpackChunkName: "translations-de-api-browser" */ 'locales/de/api_browser.json'),
  it: () => import(/* webpackChunkName: "translations-it-api-browser" */ 'locales/it/api_browser.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-api-browser" */ 'locales/zh-TW/api_browser.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-api-browser" */ 'locales/ja/api_browser.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-api-browser" */ 'locales/pl/api_browser.json'),
  es: () => import(/* webpackChunkName: "translations-es-api-browser" */ 'locales/es/api_browser.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-api-browser" */ 'locales/fr/api_browser.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-api-browser" */ 'locales/pt-BR/api_browser.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-api-browser" */ 'locales/zh-CN/api_browser.json'),
});

registerTranslations('authentication', enAuthentication, {
  de: () => import(/* webpackChunkName: "translations-de-authentication" */ 'locales/de/authentication.json'),
  it: () => import(/* webpackChunkName: "translations-it-authentication" */ 'locales/it/authentication.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-authentication" */ 'locales/zh-TW/authentication.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-authentication" */ 'locales/ja/authentication.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-authentication" */ 'locales/pl/authentication.json'),
  es: () => import(/* webpackChunkName: "translations-es-authentication" */ 'locales/es/authentication.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-authentication" */ 'locales/fr/authentication.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-authentication" */ 'locales/pt-BR/authentication.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-authentication" */ 'locales/zh-CN/authentication.json'),
});

registerTranslations('builtin_roles', enBuiltinRoles, {
  de: () => import(/* webpackChunkName: "translations-de-builtin-roles" */ 'locales/de/builtin_roles.json'),
  it: () => import(/* webpackChunkName: "translations-it-builtin-roles" */ 'locales/it/builtin_roles.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-builtin-roles" */ 'locales/zh-TW/builtin_roles.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-builtin-roles" */ 'locales/ja/builtin_roles.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-builtin-roles" */ 'locales/pl/builtin_roles.json'),
  es: () => import(/* webpackChunkName: "translations-es-builtin-roles" */ 'locales/es/builtin_roles.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-builtin-roles" */ 'locales/fr/builtin_roles.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-builtin-roles" */ 'locales/pt-BR/builtin_roles.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-builtin-roles" */ 'locales/zh-CN/builtin_roles.json'),
});

registerTranslations('certificates', enCertificates, {
  de: () => import(/* webpackChunkName: "translations-de-certificates" */ 'locales/de/certificates.json'),
  it: () => import(/* webpackChunkName: "translations-it-certificates" */ 'locales/it/certificates.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-certificates" */ 'locales/zh-TW/certificates.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-certificates" */ 'locales/ja/certificates.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-certificates" */ 'locales/pl/certificates.json'),
  es: () => import(/* webpackChunkName: "translations-es-certificates" */ 'locales/es/certificates.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-certificates" */ 'locales/fr/certificates.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-certificates" */ 'locales/pt-BR/certificates.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-certificates" */ 'locales/zh-CN/certificates.json'),
});

registerTranslations('cluster_certificate_management', enClusterCertificateManagement, {
  de: () =>
    import(
      /* webpackChunkName: "translations-de-cluster-certificate-management" */ 'locales/de/cluster_certificate_management.json'
    ),
  it: () =>
    import(
      /* webpackChunkName: "translations-it-cluster-certificate-management" */ 'locales/it/cluster_certificate_management.json'
    ),
  'zh-TW': () =>
    import(
      /* webpackChunkName: "translations-zh-tw-cluster-certificate-management" */ 'locales/zh-TW/cluster_certificate_management.json'
    ),
  ja: () =>
    import(
      /* webpackChunkName: "translations-ja-cluster-certificate-management" */ 'locales/ja/cluster_certificate_management.json'
    ),
  pl: () =>
    import(
      /* webpackChunkName: "translations-pl-cluster-certificate-management" */ 'locales/pl/cluster_certificate_management.json'
    ),
  es: () =>
    import(
      /* webpackChunkName: "translations-es-cluster-certificate-management" */ 'locales/es/cluster_certificate_management.json'
    ),
  fr: () =>
    import(
      /* webpackChunkName: "translations-fr-cluster-certificate-management" */ 'locales/fr/cluster_certificate_management.json'
    ),
  'pt-BR': () =>
    import(
      /* webpackChunkName: "translations-pt-br-cluster-certificate-management" */ 'locales/pt-BR/cluster_certificate_management.json'
    ),
  'zh-CN': () =>
    import(
      /* webpackChunkName: "translations-zh-cn-cluster-certificate-management" */ 'locales/zh-CN/cluster_certificate_management.json'
    ),
});

registerTranslations('collectors', enCollectors, {
  de: () => import(/* webpackChunkName: "translations-de-collectors" */ 'locales/de/collectors.json'),
  it: () => import(/* webpackChunkName: "translations-it-collectors" */ 'locales/it/collectors.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-collectors" */ 'locales/zh-TW/collectors.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-collectors" */ 'locales/ja/collectors.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-collectors" */ 'locales/pl/collectors.json'),
  es: () => import(/* webpackChunkName: "translations-es-collectors" */ 'locales/es/collectors.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-collectors" */ 'locales/fr/collectors.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-collectors" */ 'locales/pt-BR/collectors.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-collectors" */ 'locales/zh-CN/collectors.json'),
});

registerTranslations('common', enCommon, {
  de: () => import(/* webpackChunkName: "translations-de-common" */ 'locales/de/common.json'),
  it: () => import(/* webpackChunkName: "translations-it-common" */ 'locales/it/common.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-common" */ 'locales/zh-TW/common.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-common" */ 'locales/ja/common.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-common" */ 'locales/pl/common.json'),
  es: () => import(/* webpackChunkName: "translations-es-common" */ 'locales/es/common.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-common" */ 'locales/fr/common.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-common" */ 'locales/pt-BR/common.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-common" */ 'locales/zh-CN/common.json'),
});

registerTranslations('configurations', enConfigurations, {
  de: () => import(/* webpackChunkName: "translations-de-configurations" */ 'locales/de/configurations.json'),
  it: () => import(/* webpackChunkName: "translations-it-configurations" */ 'locales/it/configurations.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-configurations" */ 'locales/zh-TW/configurations.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-configurations" */ 'locales/ja/configurations.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-configurations" */ 'locales/pl/configurations.json'),
  es: () => import(/* webpackChunkName: "translations-es-configurations" */ 'locales/es/configurations.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-configurations" */ 'locales/fr/configurations.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-configurations" */ 'locales/pt-BR/configurations.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-configurations" */ 'locales/zh-CN/configurations.json'),
});

registerTranslations('content_packs', enContentPacks, {
  de: () => import(/* webpackChunkName: "translations-de-content-packs" */ 'locales/de/content_packs.json'),
  it: () => import(/* webpackChunkName: "translations-it-content-packs" */ 'locales/it/content_packs.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-content-packs" */ 'locales/zh-TW/content_packs.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-content-packs" */ 'locales/ja/content_packs.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-content-packs" */ 'locales/pl/content_packs.json'),
  es: () => import(/* webpackChunkName: "translations-es-content-packs" */ 'locales/es/content_packs.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-content-packs" */ 'locales/fr/content_packs.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-content-packs" */ 'locales/pt-BR/content_packs.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-content-packs" */ 'locales/zh-CN/content_packs.json'),
});

registerTranslations('dashboards', enDashboards, {
  de: () => import(/* webpackChunkName: "translations-de-dashboards" */ 'locales/de/dashboards.json'),
  it: () => import(/* webpackChunkName: "translations-it-dashboards" */ 'locales/it/dashboards.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-dashboards" */ 'locales/zh-TW/dashboards.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-dashboards" */ 'locales/ja/dashboards.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-dashboards" */ 'locales/pl/dashboards.json'),
  es: () => import(/* webpackChunkName: "translations-es-dashboards" */ 'locales/es/dashboards.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-dashboards" */ 'locales/fr/dashboards.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-dashboards" */ 'locales/pt-BR/dashboards.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-dashboards" */ 'locales/zh-CN/dashboards.json'),
});

registerTranslations('data_nodes_cluster_management', enDataNodesClusterManagement, {
  de: () =>
    import(
      /* webpackChunkName: "translations-de-data-nodes-cluster-management" */ 'locales/de/data_nodes_cluster_management.json'
    ),
  it: () =>
    import(
      /* webpackChunkName: "translations-it-data-nodes-cluster-management" */ 'locales/it/data_nodes_cluster_management.json'
    ),
  'zh-TW': () =>
    import(
      /* webpackChunkName: "translations-zh-tw-data-nodes-cluster-management" */ 'locales/zh-TW/data_nodes_cluster_management.json'
    ),
  ja: () =>
    import(
      /* webpackChunkName: "translations-ja-data-nodes-cluster-management" */ 'locales/ja/data_nodes_cluster_management.json'
    ),
  pl: () =>
    import(
      /* webpackChunkName: "translations-pl-data-nodes-cluster-management" */ 'locales/pl/data_nodes_cluster_management.json'
    ),
  es: () =>
    import(
      /* webpackChunkName: "translations-es-data-nodes-cluster-management" */ 'locales/es/data_nodes_cluster_management.json'
    ),
  fr: () =>
    import(
      /* webpackChunkName: "translations-fr-data-nodes-cluster-management" */ 'locales/fr/data_nodes_cluster_management.json'
    ),
  'pt-BR': () =>
    import(
      /* webpackChunkName: "translations-pt-br-data-nodes-cluster-management" */ 'locales/pt-BR/data_nodes_cluster_management.json'
    ),
  'zh-CN': () =>
    import(
      /* webpackChunkName: "translations-zh-cn-data-nodes-cluster-management" */ 'locales/zh-CN/data_nodes_cluster_management.json'
    ),
});

registerTranslations('datanode', enDatanode, {
  de: () => import(/* webpackChunkName: "translations-de-datanode" */ 'locales/de/datanode.json'),
  it: () => import(/* webpackChunkName: "translations-it-datanode" */ 'locales/it/datanode.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-datanode" */ 'locales/zh-TW/datanode.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-datanode" */ 'locales/ja/datanode.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-datanode" */ 'locales/pl/datanode.json'),
  es: () => import(/* webpackChunkName: "translations-es-datanode" */ 'locales/es/datanode.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-datanode" */ 'locales/fr/datanode.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-datanode" */ 'locales/pt-BR/datanode.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-datanode" */ 'locales/zh-CN/datanode.json'),
});

registerTranslations('entity_attributes', enEntityAttributes, {
  de: () => import(/* webpackChunkName: "translations-de-entity-attributes" */ 'locales/de/entity_attributes.json'),
  it: () => import(/* webpackChunkName: "translations-it-entity-attributes" */ 'locales/it/entity_attributes.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-entity-attributes" */ 'locales/zh-TW/entity_attributes.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-entity-attributes" */ 'locales/ja/entity_attributes.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-entity-attributes" */ 'locales/pl/entity_attributes.json'),
  es: () => import(/* webpackChunkName: "translations-es-entity-attributes" */ 'locales/es/entity_attributes.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-entity-attributes" */ 'locales/fr/entity_attributes.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-entity-attributes" */ 'locales/pt-BR/entity_attributes.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-entity-attributes" */ 'locales/zh-CN/entity_attributes.json'),
});

registerTranslations('event_definitions', enEventDefinitions, {
  de: () => import(/* webpackChunkName: "translations-de-event-definitions" */ 'locales/de/event_definitions.json'),
  it: () => import(/* webpackChunkName: "translations-it-event-definitions" */ 'locales/it/event_definitions.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-event-definitions" */ 'locales/zh-TW/event_definitions.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-event-definitions" */ 'locales/ja/event_definitions.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-event-definitions" */ 'locales/pl/event_definitions.json'),
  es: () => import(/* webpackChunkName: "translations-es-event-definitions" */ 'locales/es/event_definitions.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-event-definitions" */ 'locales/fr/event_definitions.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-event-definitions" */ 'locales/pt-BR/event_definitions.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-event-definitions" */ 'locales/zh-CN/event_definitions.json'),
});

registerTranslations('event_notifications', enEventNotifications, {
  de: () => import(/* webpackChunkName: "translations-de-event-notifications" */ 'locales/de/event_notifications.json'),
  it: () => import(/* webpackChunkName: "translations-it-event-notifications" */ 'locales/it/event_notifications.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-event-notifications" */ 'locales/zh-TW/event_notifications.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-event-notifications" */ 'locales/ja/event_notifications.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-event-notifications" */ 'locales/pl/event_notifications.json'),
  es: () => import(/* webpackChunkName: "translations-es-event-notifications" */ 'locales/es/event_notifications.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-event-notifications" */ 'locales/fr/event_notifications.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-event-notifications" */ 'locales/pt-BR/event_notifications.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-event-notifications" */ 'locales/zh-CN/event_notifications.json'),
});

registerTranslations('events', enEvents, {
  de: () => import(/* webpackChunkName: "translations-de-events" */ 'locales/de/events.json'),
  it: () => import(/* webpackChunkName: "translations-it-events" */ 'locales/it/events.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-events" */ 'locales/zh-TW/events.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-events" */ 'locales/ja/events.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-events" */ 'locales/pl/events.json'),
  es: () => import(/* webpackChunkName: "translations-es-events" */ 'locales/es/events.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-events" */ 'locales/fr/events.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-events" */ 'locales/pt-BR/events.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-events" */ 'locales/zh-CN/events.json'),
});

registerTranslations('extractors', enExtractors, {
  de: () => import(/* webpackChunkName: "translations-de-extractors" */ 'locales/de/extractors.json'),
  it: () => import(/* webpackChunkName: "translations-it-extractors" */ 'locales/it/extractors.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-extractors" */ 'locales/zh-TW/extractors.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-extractors" */ 'locales/ja/extractors.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-extractors" */ 'locales/pl/extractors.json'),
  es: () => import(/* webpackChunkName: "translations-es-extractors" */ 'locales/es/extractors.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-extractors" */ 'locales/fr/extractors.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-extractors" */ 'locales/pt-BR/extractors.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-extractors" */ 'locales/zh-CN/extractors.json'),
});

registerTranslations('grokpatterns', enGrokpatterns, {
  de: () => import(/* webpackChunkName: "translations-de-grokpatterns" */ 'locales/de/grokpatterns.json'),
  it: () => import(/* webpackChunkName: "translations-it-grokpatterns" */ 'locales/it/grokpatterns.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-grokpatterns" */ 'locales/zh-TW/grokpatterns.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-grokpatterns" */ 'locales/ja/grokpatterns.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-grokpatterns" */ 'locales/pl/grokpatterns.json'),
  es: () => import(/* webpackChunkName: "translations-es-grokpatterns" */ 'locales/es/grokpatterns.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-grokpatterns" */ 'locales/fr/grokpatterns.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-grokpatterns" */ 'locales/pt-BR/grokpatterns.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-grokpatterns" */ 'locales/zh-CN/grokpatterns.json'),
});

registerTranslations('index_set_templates', enIndexSetTemplates, {
  de: () => import(/* webpackChunkName: "translations-de-index-set-templates" */ 'locales/de/index_set_templates.json'),
  it: () => import(/* webpackChunkName: "translations-it-index-set-templates" */ 'locales/it/index_set_templates.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-index-set-templates" */ 'locales/zh-TW/index_set_templates.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-index-set-templates" */ 'locales/ja/index_set_templates.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-index-set-templates" */ 'locales/pl/index_set_templates.json'),
  es: () => import(/* webpackChunkName: "translations-es-index-set-templates" */ 'locales/es/index_set_templates.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-index-set-templates" */ 'locales/fr/index_set_templates.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-index-set-templates" */ 'locales/pt-BR/index_set_templates.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-index-set-templates" */ 'locales/zh-CN/index_set_templates.json'),
});

registerTranslations('indexsets', enIndexsets, {
  de: () => import(/* webpackChunkName: "translations-de-indexsets" */ 'locales/de/indexsets.json'),
  it: () => import(/* webpackChunkName: "translations-it-indexsets" */ 'locales/it/indexsets.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-indexsets" */ 'locales/zh-TW/indexsets.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-indexsets" */ 'locales/ja/indexsets.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-indexsets" */ 'locales/pl/indexsets.json'),
  es: () => import(/* webpackChunkName: "translations-es-indexsets" */ 'locales/es/indexsets.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-indexsets" */ 'locales/fr/indexsets.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-indexsets" */ 'locales/pt-BR/indexsets.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-indexsets" */ 'locales/zh-CN/indexsets.json'),
});

registerTranslations('inputs', enInputs, {
  de: () => import(/* webpackChunkName: "translations-de-inputs" */ 'locales/de/inputs.json'),
  it: () => import(/* webpackChunkName: "translations-it-inputs" */ 'locales/it/inputs.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-inputs" */ 'locales/zh-TW/inputs.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-inputs" */ 'locales/ja/inputs.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-inputs" */ 'locales/pl/inputs.json'),
  es: () => import(/* webpackChunkName: "translations-es-inputs" */ 'locales/es/inputs.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-inputs" */ 'locales/fr/inputs.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-inputs" */ 'locales/pt-BR/inputs.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-inputs" */ 'locales/zh-CN/inputs.json'),
});

registerTranslations('integrations', enIntegrations, {
  de: () => import(/* webpackChunkName: "translations-de-integrations" */ 'locales/de/integrations.json'),
  it: () => import(/* webpackChunkName: "translations-it-integrations" */ 'locales/it/integrations.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-integrations" */ 'locales/zh-TW/integrations.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-integrations" */ 'locales/ja/integrations.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-integrations" */ 'locales/pl/integrations.json'),
  es: () => import(/* webpackChunkName: "translations-es-integrations" */ 'locales/es/integrations.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-integrations" */ 'locales/fr/integrations.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-integrations" */ 'locales/pt-BR/integrations.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-integrations" */ 'locales/zh-CN/integrations.json'),
});

registerTranslations('loggers', enLoggers, {
  de: () => import(/* webpackChunkName: "translations-de-loggers" */ 'locales/de/loggers.json'),
  it: () => import(/* webpackChunkName: "translations-it-loggers" */ 'locales/it/loggers.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-loggers" */ 'locales/zh-TW/loggers.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-loggers" */ 'locales/ja/loggers.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-loggers" */ 'locales/pl/loggers.json'),
  es: () => import(/* webpackChunkName: "translations-es-loggers" */ 'locales/es/loggers.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-loggers" */ 'locales/fr/loggers.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-loggers" */ 'locales/pt-BR/loggers.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-loggers" */ 'locales/zh-CN/loggers.json'),
});

registerTranslations('login', enLogin, {
  de: () => import(/* webpackChunkName: "translations-de-login" */ 'locales/de/login.json'),
  it: () => import(/* webpackChunkName: "translations-it-login" */ 'locales/it/login.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-login" */ 'locales/zh-TW/login.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-login" */ 'locales/ja/login.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-login" */ 'locales/pl/login.json'),
  es: () => import(/* webpackChunkName: "translations-es-login" */ 'locales/es/login.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-login" */ 'locales/fr/login.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-login" */ 'locales/pt-BR/login.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-login" */ 'locales/zh-CN/login.json'),
});

registerTranslations('lookup_tables', enLookupTables, {
  de: () => import(/* webpackChunkName: "translations-de-lookup-tables" */ 'locales/de/lookup_tables.json'),
  it: () => import(/* webpackChunkName: "translations-it-lookup-tables" */ 'locales/it/lookup_tables.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-lookup-tables" */ 'locales/zh-TW/lookup_tables.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-lookup-tables" */ 'locales/ja/lookup_tables.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-lookup-tables" */ 'locales/pl/lookup_tables.json'),
  es: () => import(/* webpackChunkName: "translations-es-lookup-tables" */ 'locales/es/lookup_tables.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-lookup-tables" */ 'locales/fr/lookup_tables.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-lookup-tables" */ 'locales/pt-BR/lookup_tables.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-lookup-tables" */ 'locales/zh-CN/lookup_tables.json'),
});

registerTranslations('messageloaders', enMessageloaders, {
  de: () => import(/* webpackChunkName: "translations-de-messageloaders" */ 'locales/de/messageloaders.json'),
  it: () => import(/* webpackChunkName: "translations-it-messageloaders" */ 'locales/it/messageloaders.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-messageloaders" */ 'locales/zh-TW/messageloaders.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-messageloaders" */ 'locales/ja/messageloaders.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-messageloaders" */ 'locales/pl/messageloaders.json'),
  es: () => import(/* webpackChunkName: "translations-es-messageloaders" */ 'locales/es/messageloaders.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-messageloaders" */ 'locales/fr/messageloaders.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-messageloaders" */ 'locales/pt-BR/messageloaders.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-messageloaders" */ 'locales/zh-CN/messageloaders.json'),
});

registerTranslations('navigation', enNavigation, {
  de: () => import(/* webpackChunkName: "translations-de-navigation" */ 'locales/de/navigation.json'),
  it: () => import(/* webpackChunkName: "translations-it-navigation" */ 'locales/it/navigation.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-navigation" */ 'locales/zh-TW/navigation.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-navigation" */ 'locales/ja/navigation.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-navigation" */ 'locales/pl/navigation.json'),
  es: () => import(/* webpackChunkName: "translations-es-navigation" */ 'locales/es/navigation.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-navigation" */ 'locales/fr/navigation.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-navigation" */ 'locales/pt-BR/navigation.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-navigation" */ 'locales/zh-CN/navigation.json'),
});

registerTranslations('pages', enPages, {
  de: () => import(/* webpackChunkName: "translations-de-pages" */ 'locales/de/pages.json'),
  it: () => import(/* webpackChunkName: "translations-it-pages" */ 'locales/it/pages.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-pages" */ 'locales/zh-TW/pages.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-pages" */ 'locales/ja/pages.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-pages" */ 'locales/pl/pages.json'),
  es: () => import(/* webpackChunkName: "translations-es-pages" */ 'locales/es/pages.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-pages" */ 'locales/fr/pages.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-pages" */ 'locales/pt-BR/pages.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-pages" */ 'locales/zh-CN/pages.json'),
});

registerTranslations('pipelines', enPipelines, {
  de: () => import(/* webpackChunkName: "translations-de-pipelines" */ 'locales/de/pipelines.json'),
  it: () => import(/* webpackChunkName: "translations-it-pipelines" */ 'locales/it/pipelines.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-pipelines" */ 'locales/zh-TW/pipelines.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-pipelines" */ 'locales/ja/pipelines.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-pipelines" */ 'locales/pl/pipelines.json'),
  es: () => import(/* webpackChunkName: "translations-es-pipelines" */ 'locales/es/pipelines.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-pipelines" */ 'locales/fr/pipelines.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-pipelines" */ 'locales/pt-BR/pipelines.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-pipelines" */ 'locales/zh-CN/pipelines.json'),
});

registerTranslations('preflight', enPreflight, {
  de: () => import(/* webpackChunkName: "translations-de-preflight" */ 'locales/de/preflight.json'),
  it: () => import(/* webpackChunkName: "translations-it-preflight" */ 'locales/it/preflight.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-preflight" */ 'locales/zh-TW/preflight.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-preflight" */ 'locales/ja/preflight.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-preflight" */ 'locales/pl/preflight.json'),
  es: () => import(/* webpackChunkName: "translations-es-preflight" */ 'locales/es/preflight.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-preflight" */ 'locales/fr/preflight.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-preflight" */ 'locales/pt-BR/preflight.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-preflight" */ 'locales/zh-CN/preflight.json'),
});

registerTranslations('search', enSearch, {
  de: () => import(/* webpackChunkName: "translations-de-search" */ 'locales/de/search.json'),
  it: () => import(/* webpackChunkName: "translations-it-search" */ 'locales/it/search.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-search" */ 'locales/zh-TW/search.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-search" */ 'locales/ja/search.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-search" */ 'locales/pl/search.json'),
  es: () => import(/* webpackChunkName: "translations-es-search" */ 'locales/es/search.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-search" */ 'locales/fr/search.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-search" */ 'locales/pt-BR/search.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-search" */ 'locales/zh-CN/search.json'),
});

registerTranslations('server_errors', enServerErrors, {
  de: () => import(/* webpackChunkName: "translations-de-server-errors" */ 'locales/de/server_errors.json'),
  it: () => import(/* webpackChunkName: "translations-it-server-errors" */ 'locales/it/server_errors.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-server-errors" */ 'locales/zh-TW/server_errors.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-server-errors" */ 'locales/ja/server_errors.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-server-errors" */ 'locales/pl/server_errors.json'),
  es: () => import(/* webpackChunkName: "translations-es-server-errors" */ 'locales/es/server_errors.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-server-errors" */ 'locales/fr/server_errors.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-server-errors" */ 'locales/pt-BR/server_errors.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-server-errors" */ 'locales/zh-CN/server_errors.json'),
});

registerTranslations('server_fields', enServerFields, {
  de: () => import(/* webpackChunkName: "translations-de-server-fields" */ 'locales/de/server_fields.json'),
  it: () => import(/* webpackChunkName: "translations-it-server-fields" */ 'locales/it/server_fields.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-server-fields" */ 'locales/zh-TW/server_fields.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-server-fields" */ 'locales/ja/server_fields.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-server-fields" */ 'locales/pl/server_fields.json'),
  es: () => import(/* webpackChunkName: "translations-es-server-fields" */ 'locales/es/server_fields.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-server-fields" */ 'locales/fr/server_fields.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-server-fields" */ 'locales/pt-BR/server_fields.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-server-fields" */ 'locales/zh-CN/server_fields.json'),
});

registerTranslations('sidecars', enSidecars, {
  de: () => import(/* webpackChunkName: "translations-de-sidecars" */ 'locales/de/sidecars.json'),
  it: () => import(/* webpackChunkName: "translations-it-sidecars" */ 'locales/it/sidecars.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-sidecars" */ 'locales/zh-TW/sidecars.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-sidecars" */ 'locales/ja/sidecars.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-sidecars" */ 'locales/pl/sidecars.json'),
  es: () => import(/* webpackChunkName: "translations-es-sidecars" */ 'locales/es/sidecars.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-sidecars" */ 'locales/fr/sidecars.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-sidecars" */ 'locales/pt-BR/sidecars.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-sidecars" */ 'locales/zh-CN/sidecars.json'),
});

registerTranslations('stream_rule_types', enStreamRuleTypes, {
  de: () => import(/* webpackChunkName: "translations-de-stream-rule-types" */ 'locales/de/stream_rule_types.json'),
  it: () => import(/* webpackChunkName: "translations-it-stream-rule-types" */ 'locales/it/stream_rule_types.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-stream-rule-types" */ 'locales/zh-TW/stream_rule_types.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-stream-rule-types" */ 'locales/ja/stream_rule_types.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-stream-rule-types" */ 'locales/pl/stream_rule_types.json'),
  es: () => import(/* webpackChunkName: "translations-es-stream-rule-types" */ 'locales/es/stream_rule_types.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-stream-rule-types" */ 'locales/fr/stream_rule_types.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-stream-rule-types" */ 'locales/pt-BR/stream_rule_types.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-stream-rule-types" */ 'locales/zh-CN/stream_rule_types.json'),
});

registerTranslations('stream_rule_types_long', enStreamRuleTypesLong, {
  de: () =>
    import(/* webpackChunkName: "translations-de-stream-rule-types-long" */ 'locales/de/stream_rule_types_long.json'),
  it: () =>
    import(/* webpackChunkName: "translations-it-stream-rule-types-long" */ 'locales/it/stream_rule_types_long.json'),
  'zh-TW': () =>
    import(
      /* webpackChunkName: "translations-zh-tw-stream-rule-types-long" */ 'locales/zh-TW/stream_rule_types_long.json'
    ),
  ja: () =>
    import(/* webpackChunkName: "translations-ja-stream-rule-types-long" */ 'locales/ja/stream_rule_types_long.json'),
  pl: () =>
    import(/* webpackChunkName: "translations-pl-stream-rule-types-long" */ 'locales/pl/stream_rule_types_long.json'),
  es: () =>
    import(/* webpackChunkName: "translations-es-stream-rule-types-long" */ 'locales/es/stream_rule_types_long.json'),
  fr: () =>
    import(/* webpackChunkName: "translations-fr-stream-rule-types-long" */ 'locales/fr/stream_rule_types_long.json'),
  'pt-BR': () =>
    import(
      /* webpackChunkName: "translations-pt-br-stream-rule-types-long" */ 'locales/pt-BR/stream_rule_types_long.json'
    ),
  'zh-CN': () =>
    import(
      /* webpackChunkName: "translations-zh-cn-stream-rule-types-long" */ 'locales/zh-CN/stream_rule_types_long.json'
    ),
});

registerTranslations('streams', enStreams, {
  de: () => import(/* webpackChunkName: "translations-de-streams" */ 'locales/de/streams.json'),
  it: () => import(/* webpackChunkName: "translations-it-streams" */ 'locales/it/streams.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-streams" */ 'locales/zh-TW/streams.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-streams" */ 'locales/ja/streams.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-streams" */ 'locales/pl/streams.json'),
  es: () => import(/* webpackChunkName: "translations-es-streams" */ 'locales/es/streams.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-streams" */ 'locales/fr/streams.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-streams" */ 'locales/pt-BR/streams.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-streams" */ 'locales/zh-CN/streams.json'),
});

registerTranslations('system', enSystem, {
  de: () => import(/* webpackChunkName: "translations-de-system" */ 'locales/de/system.json'),
  it: () => import(/* webpackChunkName: "translations-it-system" */ 'locales/it/system.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-system" */ 'locales/zh-TW/system.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-system" */ 'locales/ja/system.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-system" */ 'locales/pl/system.json'),
  es: () => import(/* webpackChunkName: "translations-es-system" */ 'locales/es/system.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-system" */ 'locales/fr/system.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-system" */ 'locales/pt-BR/system.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-system" */ 'locales/zh-CN/system.json'),
});

registerTranslations('users', enUsers, {
  de: () => import(/* webpackChunkName: "translations-de-users" */ 'locales/de/users.json'),
  it: () => import(/* webpackChunkName: "translations-it-users" */ 'locales/it/users.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-users" */ 'locales/zh-TW/users.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-users" */ 'locales/ja/users.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-users" */ 'locales/pl/users.json'),
  es: () => import(/* webpackChunkName: "translations-es-users" */ 'locales/es/users.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-users" */ 'locales/fr/users.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-users" */ 'locales/pt-BR/users.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-users" */ 'locales/zh-CN/users.json'),
});

registerTranslations('welcome', enWelcome, {
  de: () => import(/* webpackChunkName: "translations-de-welcome" */ 'locales/de/welcome.json'),
  it: () => import(/* webpackChunkName: "translations-it-welcome" */ 'locales/it/welcome.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-welcome" */ 'locales/zh-TW/welcome.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-welcome" */ 'locales/ja/welcome.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-welcome" */ 'locales/pl/welcome.json'),
  es: () => import(/* webpackChunkName: "translations-es-welcome" */ 'locales/es/welcome.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-welcome" */ 'locales/fr/welcome.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-welcome" */ 'locales/pt-BR/welcome.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-welcome" */ 'locales/zh-CN/welcome.json'),
});

registerTranslations('widgets', enWidgets, {
  de: () => import(/* webpackChunkName: "translations-de-widgets" */ 'locales/de/widgets.json'),
  it: () => import(/* webpackChunkName: "translations-it-widgets" */ 'locales/it/widgets.json'),
  'zh-TW': () => import(/* webpackChunkName: "translations-zh-tw-widgets" */ 'locales/zh-TW/widgets.json'),
  ja: () => import(/* webpackChunkName: "translations-ja-widgets" */ 'locales/ja/widgets.json'),
  pl: () => import(/* webpackChunkName: "translations-pl-widgets" */ 'locales/pl/widgets.json'),
  es: () => import(/* webpackChunkName: "translations-es-widgets" */ 'locales/es/widgets.json'),
  fr: () => import(/* webpackChunkName: "translations-fr-widgets" */ 'locales/fr/widgets.json'),
  'pt-BR': () => import(/* webpackChunkName: "translations-pt-br-widgets" */ 'locales/pt-BR/widgets.json'),
  'zh-CN': () => import(/* webpackChunkName: "translations-zh-cn-widgets" */ 'locales/zh-CN/widgets.json'),
});

registerTranslations('translation_feedback', enTranslationFeedback, {
  de: () =>
    import(/* webpackChunkName: "translations-de-translation_feedback" */ 'locales/de/translation_feedback.json'),
  it: () =>
    import(/* webpackChunkName: "translations-it-translation_feedback" */ 'locales/it/translation_feedback.json'),
  'zh-TW': () =>
    import(/* webpackChunkName: "translations-zh-tw-translation_feedback" */ 'locales/zh-TW/translation_feedback.json'),
  ja: () =>
    import(/* webpackChunkName: "translations-ja-translation_feedback" */ 'locales/ja/translation_feedback.json'),
  pl: () =>
    import(/* webpackChunkName: "translations-pl-translation_feedback" */ 'locales/pl/translation_feedback.json'),
  es: () =>
    import(/* webpackChunkName: "translations-es-translation_feedback" */ 'locales/es/translation_feedback.json'),
  fr: () =>
    import(/* webpackChunkName: "translations-fr-translation_feedback" */ 'locales/fr/translation_feedback.json'),
  'pt-BR': () =>
    import(/* webpackChunkName: "translations-pt-br-translation_feedback" */ 'locales/pt-BR/translation_feedback.json'),
  'zh-CN': () =>
    import(/* webpackChunkName: "translations-zh-cn-translation_feedback" */ 'locales/zh-CN/translation_feedback.json'),
});
