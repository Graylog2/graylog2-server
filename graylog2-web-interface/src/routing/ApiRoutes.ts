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
import Qs from 'qs';

import type { TimeRange } from 'views/logic/queries/Query';

type SearchQueryString = {
  query: string,
  limit?: number,
  offset?: number,
  sort?: `${string}:${'asc' | 'desc'}`,
  decorate?: boolean,
  fields?: string,
  filter?: string,
} & Partial<TimeRange>;

const ApiRoutes = {
  AuthenticationController: {
    create: () => ({ url: '/system/authentication/services/backends' }),
    delete: (backendId: string) => ({ url: `/system/authentication/services/backends/${backendId}` }),
    disableUser: (userId: string) => ({ url: `/system/authentication/users/${userId}/disable` }),
    enableUser: (userId: string) => ({ url: `/system/authentication/users/${userId}/enable` }),
    load: (serviceId: string) => ({ url: `/system/authentication/services/backends/${serviceId}` }),
    loadActive: () => ({ url: '/system/authentication/services/active-backend' }),
    loadUsersPaginated: (authBackendId: string) => ({ url: `/system/authentication/services/backends/${authBackendId}/users` }),
    loadActiveBackendType: () => ({ url: '/system/authentication/services/backends/active-backend/type' }),
    servicesPaginated: () => ({ url: '/system/authentication/services/backends' }),
    testConnection: () => ({ url: '/system/authentication/services/test/backend/connection' }),
    testLogin: () => ({ url: '/system/authentication/services/test/backend/login' }),
    update: (serviceId: string) => ({ url: `/system/authentication/services/backends/${serviceId}` }),
    updateConfiguration: () => ({ url: '/system/authentication/services/configuration' }),
  },
  AuthzRolesController: {
    load: (roleId: string) => ({ url: `/authz/roles/${roleId}` }),
    delete: (roleId: string) => ({ url: `/authz/roles/${roleId}` }),
    list: () => ({ url: '/authz/roles' }),
    removeMember: (roleId: string, username: string) => ({ url: `/authz/roles/${roleId}/assignee/${username}` }),
    addMembers: (roleId: string) => ({ url: `/authz/roles/${roleId}/assignees` }),
    loadRolesForUser: (username: string) => ({ url: `/authz/roles/user/${username}` }),
    loadUsersForRole: (roleId: string) => ({ url: `/authz/roles/${roleId}/assignees` }),
  },
  CatalogsController: {
    showEntityIndex: () => ({ url: '/system/catalog' }),
    queryEntities: () => ({ url: '/system/catalog' }),
  },
  CodecTypesController: {
    list: () => ({ url: '/system/codecs/types/all' }),
  },
  ContentPacksController: {
    list: () => ({ url: '/system/content_packs/latest' }),
    get: (contentPackId: string) => ({ url: `/system/content_packs/${contentPackId}` }),
    getRev: (contentPackId: string, revision: string) => ({ url: `/system/content_packs/${contentPackId}/${revision}` }),
    downloadRev: (contentPackId: string, revision: number) => ({ url: `/system/content_packs/${contentPackId}/${revision}/download` }),
    create: () => ({ url: '/system/content_packs' }),
    delete: (contentPackId: string) => ({ url: `/system/content_packs/${contentPackId}` }),
    deleteRev: (contentPackId: string, revision: string) => ({ url: `/system/content_packs/${contentPackId}/${revision}` }),
    install: (contentPackId: string, revision: string) => ({ url: `/system/content_packs/${contentPackId}/${revision}/installations` }),
    installList: (contentPackId: string) => ({ url: `/system/content_packs/${contentPackId}/installations` }),
    uninstall: (contentPackId: string, installId: string) => ({ url: `/system/content_packs/${contentPackId}/installations/${installId}` }),
    uninstallDetails: (contentPackId: string, installId: string) => ({ url: `/system/content_packs/${contentPackId}/installations/${installId}/uninstall_details` }),
  },
  ClusterApiResource: {
    list: () => ({ url: '/system/cluster/nodes' }),
    node: () => ({ url: '/system/cluster/node' }),
    elasticsearchStats: () => ({ url: '/system/cluster/stats/elasticsearch' }),
  },
  ClusterConfigResource: {
    config: () => ({ url: '/system/cluster_config' }),
  },
  GrokPatternsController: {
    test: () => ({ url: '/system/grok/test' }),
    paginated: () => ({ url: '/system/grok/paginated' }),
  },
  DashboardsApiController: {
    create: () => ({ url: '/dashboards' }),
    index: () => ({ url: '/dashboards' }),
    get: (id) => ({ url: `/dashboards/${id}` }),
    delete: (id) => ({ url: `/dashboards/${id}` }),
    update: (id) => ({ url: `/dashboards/${id}` }),
    addWidget: (id) => ({ url: `/dashboards/${id}/widgets` }),
    removeWidget: (dashboardId: string, widgetId: string) => ({ url: `/dashboards/${dashboardId}/widgets/${widgetId}` }),
    widget: (dashboardId: string, widgetId: string) => ({ url: `/dashboards/${dashboardId}/widgets/${widgetId}` }),
    updateWidget: (dashboardId: string, widgetId: string) => ({ url: `/dashboards/${dashboardId}/widgets/${widgetId}` }),
    widgetValue: (dashboardId: string, widgetId: string) => ({ url: `/dashboards/${dashboardId}/widgets/${widgetId}/value` }),
    updatePositions: (dashboardId: string) => ({ url: `/dashboards/${dashboardId}/positions` }),
  },
  DecoratorsResource: {
    available: () => ({ url: '/search/decorators/available' }),
    create: () => ({ url: '/search/decorators' }),
    get: () => ({ url: '/search/decorators' }),
    remove: (decoratorId: string) => ({ url: `/search/decorators/${decoratorId}` }),
    update: (decoratorId: string) => ({ url: `/search/decorators/${decoratorId}` }),
  },
  DeflectorApiController: {
    cycle: (indexSetId: string) => ({ url: `/cluster/deflector/${indexSetId}/cycle` }),
    list: (indexSetId: string) => ({ url: `/system/deflector/${indexSetId}` }),
  },
  EntityScopeController: {
    getScope: () => ({ url: '/entity_scopes' }),
  },
  EntityShareController: {
    prepare: (entityGRN: string) => ({ url: `/authz/shares/entities/${entityGRN}/prepare` }),
    update: (entityGRN: string) => ({ url: `/authz/shares/entities/${entityGRN}` }),
    userSharesPaginated: (username: string) => ({ url: `/authz/shares/user/${username}` }),
    entityScopes: () => ({ url: '/entity_scopes' }),
  },
  EventDefinitionsApiController: {
    list: () => ({ url: '/events/definitions' }),
    paginated: () => ({ url: '/events/definitions/paginated' }),
    get: (definitionId: string) => ({ url: `/events/definitions/${definitionId}` }),
    create: () => ({ url: '/events/definitions' }),
    bulkDelete: () => ({ url: '/events/definitions/bulk_delete' }),
    bulkSchedule: () => ({ url: '/events/definitions/bulk_schedule' }),
    bulkUnschedule: () => ({ url: '/events/definitions/bulk_unschedule' }),
    update: (definitionId: string) => ({ url: `/events/definitions/${definitionId}` }),
    delete: (definitionId: string) => ({ url: `/events/definitions/${definitionId}` }),
  },
  EventNotificationsApiController: {
    list: () => ({ url: '/events/notifications' }),
    paginated: () => ({ url: '/events/notifications/paginated' }),
    get: (definitionId: string) => ({ url: `/events/notifications/${definitionId}` }),
    create: () => ({ url: '/events/notifications' }),
    bulkDelete: () => ({ url: '/events/notifications/bulk_delete' }),
    bulkSchedule: () => ({ url: '/events/notifications/bulk_schedule' }),
    bulkUnschedule: () => ({ url: '/events/notifications/bulk_unschedule' }),
    update: (definitionId: string) => ({ url: `/events/notifications/${definitionId}` }),
    delete: (definitionId: string) => ({ url: `/events/notifications/${definitionId}` }),
  },
  HTTPHeaderAuthConfigController: {
    load: () => ({ url: '/system/authentication/http-header-auth-config' }),
    update: () => ({ url: '/system/authentication/http-header-auth-config' }),
  },
  IndexerClusterApiController: {
    health: () => ({ url: '/system/indexer/cluster/health' }),
    name: () => ({ url: '/system/indexer/cluster/name' }),
    info: () => ({ url: '/system/indexer/cluster/info' }),
  },
  IndexerFailuresApiController: {
    count: (since: number) => ({ url: `/system/indexer/failures/count?since=${since}` }),
    list: (limit: number, offset: number) => ({ url: `/system/indexer/failures?limit=${limit}&offset=${offset}` }),
  },
  IndexerOverviewApiResource: {
    list: (indexSetId: string) => ({ url: `/system/indexer/overview/${indexSetId}` }),
  },
  IndexRangesApiController: {
    list: () => ({ url: '/system/indices/ranges' }),
    rebuild: (indexSetId: string) => ({ url: `/system/indices/ranges/index_set/${indexSetId}/rebuild` }),
    rebuildSingle: (index: string) => ({ url: `/system/indices/ranges/${index}/rebuild` }),
  },
  IndexSetsApiController: {
    list: (stats) => ({ url: `/system/indices/index_sets?stats=${stats}` }),
    listPaginated: (skip, limit, stats) => ({ url: `/system/indices/index_sets?skip=${skip}&limit=${limit}&stats=${stats}` }),
    get: (indexSetId: string) => ({ url: `/system/indices/index_sets/${indexSetId}` }),
    getIndexSetStats: (indexSetId: string) => ({ url: `/system/indices/index_sets/${indexSetId}/stats` }),
    create: () => ({ url: '/system/indices/index_sets' }),
    delete: (indexSetId: string, deleteIndices) => ({ url: `/system/indices/index_sets/${indexSetId}?delete_indices=${deleteIndices}` }),
    searchPaginated: (searchTerm, skip, limit, stats) => ({ url: `/system/indices/index_sets/search?searchTitle=${searchTerm}&skip=${skip}&limit=${limit}&stats=${stats}` }),
    setDefault: (indexSetId: string) => ({ url: `/system/indices/index_sets/${indexSetId}/default` }),
    stats: () => ({ url: '/system/indices/index_sets/stats' }),
  },
  IndicesApiController: {
    close: (indexName: string) => ({ url: `/system/indexer/indices/${indexName}/close` }),
    delete: (indexName: string) => ({ url: `/system/indexer/indices/${indexName}` }),
    list: (indexSetId: string) => ({ url: `/system/indexer/indices/${indexSetId}/list` }),
    listAll: () => ({ url: '/system/indexer/indices' }),
    listClosed: (indexSetId: string) => ({ url: `/system/indexer/indices/${indexSetId}/closed` }),
    multiple: () => ({ url: '/system/indexer/indices/multiple' }),
    reopen: (indexName: string) => ({ url: `/system/indexer/indices/${indexName}/reopen` }),
  },
  InputsApiController: {
    list: () => ({ url: '/system/inputs' }),
    get: (id: string) => ({ url: `/system/inputs/${id}` }),
    globalRecentMessage: (inputId: string) => ({ url: `/${inputId}` }),
  },
  InputStatesController: {
    start: (inputId: string) => ({ url: `/system/inputstates/${inputId}` }),
    stop: (inputId: string) => ({ url: `/system/inputstates/${inputId}` }),
  },
  ClusterInputStatesController: {
    list: () => ({ url: '/cluster/inputstates' }),
    start: (inputId: string) => ({ url: `/cluster/inputstates/${inputId}` }),
    stop: (inputId: string) => ({ url: `/cluster/inputstates/${inputId}` }),
    setup: (inputId: string) => ({ url: `/cluster/inputstates/setup/${inputId}` }),
  },
  ClusterLoggersResource: {
    loggers: () => ({ url: '/cluster/system/loggers' }),
    subsystems: () => ({ url: '/cluster/system/loggers/subsystems' }),
    setSubsystemLoggerLevel: (nodeId: string, subsystem: string, loglevel: string) => ({ url: `/cluster/system/loggers/${nodeId}/subsystems/${subsystem}/level/${loglevel}` }),
  },
  ClusterSupportBundleController: {
    delete: (filename: string) => ({ url: `/cluster/debug/support/bundle/${filename}` }),
    download: (filename: string) => ({ url: `/cluster/debug/support/bundle/download/${filename}` }),
    list: () => ({ url: '/cluster/debug/support/bundle/list' }),
    create: () => ({ url: '/cluster/debug/support/bundle/build' }),
  },
  MessageFieldsApiController: {
    list: () => ({ url: '/system/fields' }),
    types: () => ({ url: 'views/fields' }),
  },
  MetricsApiController: {
    multiple: () => ({ url: '/system/metrics/multiple' }),
    byNamespace: (namespace: string) => ({ url: `/system/metrics/namespace/${namespace}` }),
  },
  ClusterMetricsApiController: {
    multiple: (nodeId: string) => ({ url: `/cluster/${nodeId}/metrics/multiple` }),
    multipleAllNodes: () => ({ url: '/cluster/metrics/multiple' }),
    byNamespace: (nodeId: string, namespace: string) => ({ url: `/cluster/${nodeId}/metrics/namespace/${namespace}` }),
  },
  NotificationsApiController: {
    delete: (type: string) => ({ url: `/system/notifications/${type}` }),
    deleteWithKey: (type: string, key: string) => ({ url: `/system/notifications/${type}/${key}` }),
    list: () => ({ url: '/system/notifications' }),
    getHtmlMessage: (type: string) => ({ url: `/system/notification/message/html/${type.toLocaleUpperCase()}` }),
    getHtmlMessageWithKey: (type: string, key: string) => ({ url: `/system/notification/message/html/${type.toLocaleUpperCase()}/${key}` }),
  },
  OutputsApiController: {
    index: () => ({ url: '/system/outputs' }),
    create: () => ({ url: '/system/outputs' }),
    delete: (outputId: string) => ({ url: `/system/outputs/${outputId}` }),
    update: (outputId: string) => ({ url: `/system/outputs/${outputId}` }),
    availableType: (type: string) => ({ url: `/system/outputs/available/${type}` }),
    availableTypes: () => ({ url: '/system/outputs/available' }),
  },
  SavedSearchesApiController: {
    create: () => ({ url: '/search/saved' }),
    delete: (savedSearchId: string) => ({ url: `/search/saved/${savedSearchId}` }),
    update: (savedSearchId: string) => ({ url: `/search/saved/${savedSearchId}` }),
  },
  SessionsApiController: {
    validate: () => ({ url: '/system/sessions' }),
  },
  StreamsApiController: {
    index: () => ({ url: '/streams' }),
    paginated: () => ({ url: '/streams/paginated' }),
    get: (streamId: string) => ({ url: `/streams/${streamId}` }),
    bulk_delete: () => ({ url: '/streams/bulk_delete' }),
    bulk_resume: () => ({ url: '/streams/bulk_resume' }),
    bulk_pause: () => ({ url: '/streams/bulk_pause' }),
    create: () => ({ url: '/streams' }),
    update: (streamId: string) => ({ url: `/streams/${streamId}` }),
    cloneStream: (streamId: string) => ({ url: `/streams/${streamId}/clone` }),
    delete: (streamId: string) => ({ url: `/streams/${streamId}` }),
    pause: (streamId: string) => ({ url: `/streams/${streamId}/pause` }),
    resume: (streamId: string) => ({ url: `/streams/${streamId}/resume` }),
    testMatch: (streamId: string) => ({ url: `/streams/${streamId}/testMatch` }),
  },
  StreamOutputsApiController: {
    add: (streamId: string) => ({ url: `/streams/${streamId}/outputs` }),
    index: (streamId: string) => ({ url: `/streams/${streamId}/outputs` }),
    delete: (streamId: string, outputId: string) => ({ url: `/streams/${streamId}/outputs/${outputId}` }),
  },
  StreamRulesApiController: {
    delete: (streamId: string, streamRuleId: string) => ({ url: `/streams/${streamId}/rules/${streamRuleId}` }),
    update: (streamId: string, streamRuleId: string) => ({ url: `/streams/${streamId}/rules/${streamRuleId}` }),
    create: (streamId: string) => ({ url: `/streams/${streamId}/rules` }),
  },
  StreamOutputFilterRuleApiController: {
    get: (streamId: string) => ({ url: `/streams/${streamId}/destinations/filters` }),
    delete: (streamId: string, filterId: string) => ({ url: `/streams/${streamId}/destinations/filters/${filterId}` }),
    update: (streamId: string, filterId: string) => ({ url: `/streams/${streamId}/destinations/filters/${filterId}` }),
    create: (streamId: string) => ({ url: `/streams/${streamId}/destinations/filters` }),
  },
  SystemApiController: {
    info: () => ({ url: '/system' }),
    jvm: () => ({ url: '/system/jvm' }),
    fields: () => ({ url: '/system/fields' }),
    locales: () => ({ url: '/system/locales' }),
  },
  SystemJobsApiController: {
    list: () => ({ url: '/cluster/jobs' }),
    getJob: (jobId: string) => ({ url: `/cluster/jobs/${jobId}` }),
    acknowledgeJob: (jobId: string) => ({ url: `/system/jobs/acknowledge/${jobId}` }),
    cancelJob: (jobId: string) => ({ url: `/cluster/jobs/${jobId}` }),
  },
  SystemMessagesApiController: {
    all: (page: number) => ({ url: `/system/messages?page=${page}` }),
  },
  SystemSearchVersionApiController: {
    satisfiesVersion: (distribution: 'opensearch' | 'elasticsearch', version?: string) => ({ url: `/system/searchVersion/satisfiesVersion/${distribution}${version ? `?version=${version}` : ''}` }),
  },
  ToolsApiController: {
    grokTest: () => ({ url: '/tools/grok_tester' }),
    jsonTest: () => ({ url: '/tools/json_tester' }),
    naturalDateTest: (string, timezone) => ({ url: `/tools/natural_date_tester?string=${string}&timezone=${timezone}` }),
    regexTest: () => ({ url: '/tools/regex_tester' }),
    regexValidate: (regex: string) => ({ url: `/tools/regex_tester/validate?regex=${regex}` }),
    regexReplaceTest: () => ({ url: '/tools/regex_replace_tester' }),
    splitAndIndexTest: () => ({ url: '/tools/split_and_index_tester' }),
    substringTest: () => ({ url: '/tools/substring_tester' }),
    containsStringTest: () => ({ url: '/tools/contains_string_tester' }),
    lookupTableTest: () => ({ url: '/tools/lookup_table_tester' }),
    urlWhitelistCheck: () => ({ url: '/system/urlwhitelist/check' }),
    urlWhitelistGenerateRegex: () => ({ url: '/system/urlwhitelist/generate_regex' }),
  },
  TelemetryApiController: {
    info: () => ({ url: '/telemetry' }),
    setting: () => ({ url: '/telemetry/user/settings' }),
  },
  UniversalSearchApiController: {
    _streamFilter(streamId: string) {
      return (streamId ? { filter: `streams:${streamId}` } : {});
    },
    _buildBaseQueryString(query: string, timerange: TimeRange, streamId): SearchQueryString {
      const queryString: Partial<SearchQueryString> = {};

      const streamFilter = this._streamFilter(streamId);

      queryString.query = query;

      Object.keys(timerange).forEach((key) => {
        queryString[key] = timerange[key];
      });

      Object.keys(streamFilter).forEach((key) => {
        queryString[key] = streamFilter[key];
      });

      return queryString as SearchQueryString;
    },
    _buildUrl(url: string, queryString: SearchQueryString) {
      return `${url}?${Qs.stringify(queryString)}`;
    },
    search(type: string, query: string, timerange: TimeRange, streamId: string, limit: number, offset: number, sortField: string, sortOrder: 'asc' | 'desc', decorate: boolean) {
      const url = `/search/universal/${type}`;
      const queryString = this._buildBaseQueryString(query, timerange, streamId);

      if (limit) {
        queryString.limit = limit;
      }

      if (offset) {
        queryString.offset = offset;
      }

      if (sortField && sortOrder) {
        queryString.sort = `${sortField}:${sortOrder}`;
      }

      if (decorate !== undefined) {
        queryString.decorate = decorate;
      }

      return { url: this._buildUrl(url, queryString) };
    },
    export(type: string, query: string, timerange: TimeRange, streamId: string, limit: number, offset: number, fields: Array<string>) {
      const url = `/search/universal/${type}/export`;
      const queryString = this._buildBaseQueryString(query, timerange, streamId);

      if (limit) {
        queryString.limit = limit;
      }

      if (offset) {
        queryString.offset = offset;
      }

      if (fields) {
        queryString.fields = fields.join(',');
      }

      return { url: this._buildUrl(url, queryString) };
    },
  },
  UsersApiController: {
    changePassword: (userId: string) => ({ url: `/users/${userId}/password` }),
    create: () => ({ url: '/users' }),
    list: () => ({ url: '/users' }),
    paginated: () => ({ url: '/users/paginated' }),
    load: (userId: string) => ({ url: `/users/id/${userId}` }),
    loadByUsername: (username: string) => ({ url: `/users/${username}` }),
    delete: (id: string) => ({ url: `/users/id/${id}` }),
    update: (userId: string) => ({ url: `/users/${userId}` }),
    create_token: (userId: string, tokenName: string) => ({ url: `/users/${userId}/tokens/${tokenName}` }),
    delete_token: (userId: string, tokenName: string) => ({ url: `/users/${userId}/tokens/${tokenName}` }),
    list_tokens: (userId: string) => ({ url: `/users/${userId}/tokens` }),
    setStatus: (userId: string, accountStatus) => ({ url: `/users/${userId}/status/${accountStatus}` }),
  },
  DashboardsController: {
    show: (id) => ({ url: `/dashboards/${id}` }),
  },
  ExtractorsController: {
    create: (inputId: string) => ({ url: `/system/inputs/${inputId}/extractors` }),
    delete: (inputId: string, extractorId: string) => ({ url: `/system/inputs/${inputId}/extractors/${extractorId}` }),
    order: (inputId: string) => ({ url: `/system/inputs/${inputId}/extractors/order` }),
    update: (inputId: string, extractorId: string) => ({ url: `/system/inputs/${inputId}/extractors/${extractorId}` }),
  },
  MessagesController: {
    analyze: (index: string, string: string) => ({ url: `/messages/${index}/analyze?string=${string}` }),
    parse: () => ({ url: '/messages/parse' }),
    single: (index: string, messageId: string) => ({ url: `/messages/${index}/${messageId}` }),
    exportSearch: ((searchId: string) => ({ url: `/views/search/messages/${searchId}` })),
    exportSearchType: ((searchId: string, searchTypeId: string) => ({ url: `/views/search/messages/${searchId}/${searchTypeId}` })),
    jobResults: ((exportJobId: string, filename: string) => ({ url: `/views/search/messages/job/${exportJobId}/${filename}` })),
  },
  ExportJobsController: {
    exportSearch: ((searchId: string) => ({ url: `/views/export/${searchId}` })),
    exportSearchType: ((searchId: string, searchTypeId: string) => ({ url: `/views/export/${searchId}/${searchTypeId}` })),
  },
  MapDataController: {
    search: () => ({ url: '/search/mapdata' }),
  },
  PipelinesController: {
    list: () => ({ url: '/system/pipelines/pipeline' }),
    paginatedList: () => ({ url: '/system/pipelines/pipeline/paginated' }),
    create: () => ({ url: '/system/pipelines/pipeline' }),
    get: (pipelineId: string) => ({ url: `/system/pipelines/pipeline/${pipelineId}` }),
    update: (pipelineId: string) => ({ url: `/system/pipelines/pipeline/${pipelineId}` }),
    delete: (pipelineId: string) => ({ url: `/system/pipelines/pipeline/${pipelineId}` }),
    parse: () => ({ url: '/system/pipelines/pipeline/parse' }),
    updateRouting: () => ({ url: '/system/pipelines/pipeline/routing' }),
  },
  RulesController: {
    list: () => ({ url: '/system/pipelines/rule' }),
    paginatedList: () => ({ url: '/system/pipelines/rule/paginated' }),
    create: () => ({ url: '/system/pipelines/rule' }),
    get: (ruleId: string) => ({ url: `/system/pipelines/rule/${ruleId}` }),
    update: (ruleId: string) => ({ url: `/system/pipelines/rule/${ruleId}` }),
    delete: (ruleId: string) => ({ url: `/system/pipelines/rule/${ruleId}` }),
    multiple: () => ({ url: '/system/pipelines/rule/multiple' }),
    functions: () => ({ url: '/system/pipelines/rule/functions' }),
    parse: () => ({ url: '/system/pipelines/rule/parse' }),
    simulate: () => ({ url: '/system/pipelines/rule/simulate' }),
    metricsConfig: () => ({ url: '/system/pipelines/rule/config/metrics' }),
  },
  RuleBuilderController: {
    create: () => ({ url: '/system/pipelines/rulebuilder' }),
    update: (ruleId: string) => ({ url: `/system/pipelines/rulebuilder/${ruleId}` }),
    validate: () => ({ url: '/system/pipelines/rulebuilder/validate' }),
    simulate: () => ({ url: '/system/pipelines/rulebuilder/simulate' }),
    listConditionsDict: () => ({ url: '/system/pipelines/rulebuilder/conditions' }),
    listActionsDict: () => ({ url: '/system/pipelines/rulebuilder/actions' }),
  },
  ConnectionsController: {
    list: () => ({ url: '/system/pipelines/connections' }),
    to_stream: () => ({ url: '/system/pipelines/connections/to_stream' }),
    to_pipeline: () => ({ url: '/system/pipelines/connections/to_pipeline' }),
  },
  SimulatorController: {
    simulate: () => ({ url: '/system/pipelines/simulate' }),
  },
  ping: () => ({ url: '/' }),
};

export default ApiRoutes;
