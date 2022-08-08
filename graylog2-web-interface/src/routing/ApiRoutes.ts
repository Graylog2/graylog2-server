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
  sort?: `${string}:${'asc'|'desc'}`,
  decorate?: boolean,
  fields?: string,
  filter?: string,
} & Partial<TimeRange>;

const ApiRoutes = {
  AlarmCallbacksApiController: {
    available: () => { return { url: '/alerts/callbacks/types' }; },
    create: (streamId: string) => { return { url: `/streams/${streamId}/alarmcallbacks` }; },
    delete: (streamId: string, alarmCallbackId: string) => { return { url: `/streams/${streamId}/alarmcallbacks/${alarmCallbackId}` }; },
    listAll: () => { return { url: '/alerts/callbacks' }; },
    list: (streamId: string) => { return { url: `/streams/${streamId}/alarmcallbacks` }; },
    testAlert: (alarmCallbackId: string) => { return { url: `/alerts/callbacks/${alarmCallbackId}/test` }; },
    update: (streamId: string, alarmCallbackId: string) => { return { url: `/streams/${streamId}/alarmcallbacks/${alarmCallbackId}` }; },
  },
  AlarmCallbackHistoryApiController: {
    list: (streamId: string, alertId: string) => { return { url: `/streams/${streamId}/alerts/${alertId}/history` }; },
  },
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
    load: (roleId: string) => { return { url: `/authz/roles/${roleId}` }; },
    delete: (roleId: string) => { return { url: `/authz/roles/${roleId}` }; },
    list: () => { return { url: '/authz/roles' }; },
    removeMember: (roleId: string, username: string) => { return { url: `/authz/roles/${roleId}/assignee/${username}` }; },
    addMembers: (roleId: string) => { return { url: `/authz/roles/${roleId}/assignees` }; },
    loadRolesForUser: (username: string) => { return { url: `/authz/roles/user/${username}` }; },
    loadUsersForRole: (roleId: string) => { return { url: `/authz/roles/${roleId}/assignees` }; },
  },
  CatalogsController: {
    showEntityIndex: () => { return { url: '/system/catalog' }; },
    queryEntities: () => { return { url: '/system/catalog' }; },
  },
  CodecTypesController: {
    list: () => { return { url: '/system/codecs/types/all' }; },
  },
  ContentPacksController: {
    list: () => { return { url: '/system/content_packs/latest' }; },
    get: (contentPackId: string) => { return { url: `/system/content_packs/${contentPackId}` }; },
    getRev: (contentPackId: string, revision: string) => { return { url: `/system/content_packs/${contentPackId}/${revision}` }; },
    downloadRev: (contentPackId: string, revision: string) => { return { url: `/system/content_packs/${contentPackId}/${revision}/download` }; },
    create: () => { return { url: '/system/content_packs' }; },
    delete: (contentPackId: string) => { return { url: `/system/content_packs/${contentPackId}` }; },
    deleteRev: (contentPackId: string, revision: string) => { return { url: `/system/content_packs/${contentPackId}/${revision}` }; },
    install: (contentPackId: string, revision: string) => { return { url: `/system/content_packs/${contentPackId}/${revision}/installations` }; },
    installList: (contentPackId: string) => { return { url: `/system/content_packs/${contentPackId}/installations` }; },
    uninstall: (contentPackId: string, installId: string) => { return { url: `/system/content_packs/${contentPackId}/installations/${installId}` }; },
    uninstallDetails: (contentPackId: string, installId: string) => { return { url: `/system/content_packs/${contentPackId}/installations/${installId}/uninstall_details` }; },
  },
  CountsApiController: {
    total: () => { return { url: '/count/total' }; },
    indexSetTotal: (indexSetId: string) => { return { url: `/count/${indexSetId}/total` }; },
  },
  ClusterApiResource: {
    list: () => { return { url: '/system/cluster/nodes' }; },
    node: () => { return { url: '/system/cluster/node' }; },
    elasticsearchStats: () => { return { url: '/system/cluster/stats/elasticsearch' }; },
  },
  ClusterConfigResource: {
    config: () => { return { url: '/system/cluster_config' }; },
  },
  GrokPatternsController: {
    test: () => { return { url: '/system/grok/test' }; },
    paginated: () => { return { url: '/system/grok/paginated' }; },
  },
  DashboardsApiController: {
    create: () => { return { url: '/dashboards' }; },
    index: () => { return { url: '/dashboards' }; },
    get: (id) => { return { url: `/dashboards/${id}` }; },
    delete: (id) => { return { url: `/dashboards/${id}` }; },
    update: (id) => { return { url: `/dashboards/${id}` }; },
    addWidget: (id) => { return { url: `/dashboards/${id}/widgets` }; },
    removeWidget: (dashboardId: string, widgetId: string) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    widget: (dashboardId: string, widgetId: string) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    updateWidget: (dashboardId: string, widgetId: string) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    widgetValue: (dashboardId: string, widgetId: string) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}/value` }; },
    updatePositions: (dashboardId: string) => { return { url: `/dashboards/${dashboardId}/positions` }; },
  },
  DecoratorsResource: {
    available: () => { return { url: '/search/decorators/available' }; },
    create: () => { return { url: '/search/decorators' }; },
    get: () => { return { url: '/search/decorators' }; },
    remove: (decoratorId: string) => { return { url: `/search/decorators/${decoratorId}` }; },
    update: (decoratorId: string) => { return { url: `/search/decorators/${decoratorId}` }; },
  },
  DeflectorApiController: {
    cycle: (indexSetId: string) => { return { url: `/cluster/deflector/${indexSetId}/cycle` }; },
    list: (indexSetId: string) => { return { url: `/system/deflector/${indexSetId}` }; },
  },
  EntityScopeController: {
    getScope: () => { return { url: '/entity_scopes' }; },
  },
  EntityShareController: {
    prepare: (entityGRN: string) => { return { url: `/authz/shares/entities/${entityGRN}/prepare` }; },
    update: (entityGRN: string) => { return { url: `/authz/shares/entities/${entityGRN}` }; },
    userSharesPaginated: (username: string) => { return { url: `/authz/shares/user/${username}` }; },
    entityScopes: () => { return { url: '/entity_scopes' }; },
  },
  EntityScopeController: {
    getScope: () => ({ url: '/entity_scopes' }),
  },
  HTTPHeaderAuthConfigController: {
    load: () => ({ url: '/system/authentication/http-header-auth-config' }),
    update: () => ({ url: '/system/authentication/http-header-auth-config' }),
  },
  IndexerClusterApiController: {
    health: () => { return { url: '/system/indexer/cluster/health' }; },
    name: () => { return { url: '/system/indexer/cluster/name' }; },
  },
  IndexerFailuresApiController: {
    count: (since: number) => { return { url: `/system/indexer/failures/count?since=${since}` }; },
    list: (limit: number, offset: number) => { return { url: `/system/indexer/failures?limit=${limit}&offset=${offset}` }; },
  },
  IndexerOverviewApiResource: {
    list: (indexSetId: string) => { return { url: `/system/indexer/overview/${indexSetId}` }; },
  },
  IndexRangesApiController: {
    list: () => { return { url: '/system/indices/ranges' }; },
    rebuild: (indexSetId: string) => { return { url: `/system/indices/ranges/index_set/${indexSetId}/rebuild` }; },
    rebuildSingle: (index: string) => { return { url: `/system/indices/ranges/${index}/rebuild` }; },
  },
  IndexSetsApiController: {
    list: (stats) => { return { url: `/system/indices/index_sets?stats=${stats}` }; },
    listPaginated: (skip, limit, stats) => { return { url: `/system/indices/index_sets?skip=${skip}&limit=${limit}&stats=${stats}` }; },
    get: (indexSetId: string) => { return { url: `/system/indices/index_sets/${indexSetId}` }; },
    create: () => { return { url: '/system/indices/index_sets' }; },
    delete: (indexSetId: string, deleteIndices) => { return { url: `/system/indices/index_sets/${indexSetId}?delete_indices=${deleteIndices}` }; },
    setDefault: (indexSetId: string) => { return { url: `/system/indices/index_sets/${indexSetId}/default` }; },
    stats: () => { return { url: '/system/indices/index_sets/stats' }; },
  },
  IndicesApiController: {
    close: (indexName: string) => { return { url: `/system/indexer/indices/${indexName}/close` }; },
    delete: (indexName: string) => { return { url: `/system/indexer/indices/${indexName}` }; },
    list: (indexSetId: string) => { return { url: `/system/indexer/indices/${indexSetId}/list` }; },
    listAll: () => { return { url: '/system/indexer/indices' }; },
    listClosed: (indexSetId: string) => { return { url: `/system/indexer/indices/${indexSetId}/closed` }; },
    multiple: () => { return { url: '/system/indexer/indices/multiple' }; },
    reopen: (indexName: string) => { return { url: `/system/indexer/indices/${indexName}/reopen` }; },
  },
  InputsApiController: {
    list: () => { return { url: '/system/inputs' }; },
    get: (id: string) => { return { url: `/system/inputs/${id}` }; },
    globalRecentMessage: (inputId: string) => { return { url: `/${inputId}` }; },
  },
  InputStatesController: {
    start: (inputId: string) => { return { url: `/system/inputstates/${inputId}` }; },
    stop: (inputId: string) => { return { url: `/system/inputstates/${inputId}` }; },
  },
  ClusterInputStatesController: {
    list: () => { return { url: '/cluster/inputstates' }; },
    start: (inputId: string) => { return { url: `/cluster/inputstates/${inputId}` }; },
    stop: (inputId: string) => { return { url: `/cluster/inputstates/${inputId}` }; },
  },
  ClusterLoggersResource: {
    loggers: () => { return { url: '/cluster/system/loggers' }; },
    subsystems: () => { return { url: '/cluster/system/loggers/subsystems' }; },
    setSubsystemLoggerLevel: (nodeId: string, subsystem: string, loglevel: string) => { return { url: `/cluster/system/loggers/${nodeId}/subsystems/${subsystem}/level/${loglevel}` }; },
  },
  MessageFieldsApiController: {
    list: () => { return { url: '/system/fields' }; },
    types: () => ({ url: 'views/fields' }),
  },
  MetricsApiController: {
    multiple: () => { return { url: '/system/metrics/multiple' }; },
    byNamespace: (namespace: string) => { return { url: `/system/metrics/namespace/${namespace}` }; },
  },
  ClusterMetricsApiController: {
    multiple: (nodeId: string) => { return { url: `/cluster/${nodeId}/metrics/multiple` }; },
    multipleAllNodes: () => { return { url: '/cluster/metrics/multiple' }; },
    byNamespace: (nodeId: string, namespace: string) => { return { url: `/cluster/${nodeId}/metrics/namespace/${namespace}` }; },
  },
  NotificationsApiController: {
    delete: (type: string) => { return { url: `/system/notifications/${type}` }; },
    list: () => { return { url: '/system/notifications' }; },
  },
  OutputsApiController: {
    index: () => { return { url: '/system/outputs' }; },
    create: () => { return { url: '/system/outputs' }; },
    delete: (outputId: string) => { return { url: `/system/outputs/${outputId}` }; },
    update: (outputId: string) => { return { url: `/system/outputs/${outputId}` }; },
    availableType: (type: string) => { return { url: `/system/outputs/available/${type}` }; },
    availableTypes: () => { return { url: '/system/outputs/available' }; },
  },
  RolesApiController: {
    listRoles: () => { return { url: '/roles' }; },
    createRole: () => { return { url: '/roles' }; },
    updateRole: (rolename: string) => { return { url: `/roles/${rolename}` }; },
    deleteRole: (rolename: string) => { return { url: `/roles/${rolename}` }; },
    loadMembers: (rolename: string) => { return { url: `/roles/${rolename}/members` }; },
  },
  SavedSearchesApiController: {
    create: () => { return { url: '/search/saved' }; },
    delete: (savedSearchId: string) => { return { url: `/search/saved/${savedSearchId}` }; },
    update: (savedSearchId: string) => { return { url: `/search/saved/${savedSearchId}` }; },
  },
  SessionsApiController: {
    validate: () => { return { url: '/system/sessions' }; },
  },
  StreamsApiController: {
    index: () => { return { url: '/streams' }; },
    paginated: () => { return { url: '/streams/paginated' }; },
    get: (streamId: string) => { return { url: `/streams/${streamId}` }; },
    create: () => { return { url: '/streams' }; },
    update: (streamId: string) => { return { url: `/streams/${streamId}` }; },
    cloneStream: (streamId: string) => { return { url: `/streams/${streamId}/clone` }; },
    delete: (streamId: string) => { return { url: `/streams/${streamId}` }; },
    pause: (streamId: string) => { return { url: `/streams/${streamId}/pause` }; },
    resume: (streamId: string) => { return { url: `/streams/${streamId}/resume` }; },
    testMatch: (streamId: string) => { return { url: `/streams/${streamId}/testMatch` }; },
  },
  StreamOutputsApiController: {
    add: (streamId: string) => { return { url: `/streams/${streamId}/outputs` }; },
    index: (streamId: string) => { return { url: `/streams/${streamId}/outputs` }; },
    delete: (streamId: string, outputId: string) => { return { url: `/streams/${streamId}/outputs/${outputId}` }; },
  },
  StreamRulesApiController: {
    delete: (streamId: string, streamRuleId: string) => { return { url: `/streams/${streamId}/rules/${streamRuleId}` }; },
    update: (streamId: string, streamRuleId: string) => { return { url: `/streams/${streamId}/rules/${streamRuleId}` }; },
    create: (streamId: string) => { return { url: `/streams/${streamId}/rules` }; },
  },
  SystemApiController: {
    info: () => { return { url: '/system' }; },
    jvm: () => { return { url: '/system/jvm' }; },
    fields: () => { return { url: '/system/fields' }; },
    locales: () => { return { url: '/system/locales' }; },
  },
  SystemJobsApiController: {
    list: () => { return { url: '/cluster/jobs' }; },
    getJob: (jobId: string) => { return { url: `/cluster/jobs/${jobId}` }; },
    acknowledgeJob: (jobId: string) => { return { url: `/system/jobs/acknowledge/${jobId}` }; },
    cancelJob: (jobId: string) => { return { url: `/cluster/jobs/${jobId}` }; },
  },
  SystemMessagesApiController: {
    all: (page: number) => { return { url: `/system/messages?page=${page}` }; },
  },
  SystemSearchVersionApiController: {
    satisfiesVersion: (distribution: 'opensearch' | 'elasticsearch', version?: string) => {
      return { url: `/system/searchVersion/satisfiesVersion/${distribution}${version ? `?version=${version}` : ''}` };
    },
  },
  ToolsApiController: {
    grokTest: () => { return { url: '/tools/grok_tester' }; },
    jsonTest: () => { return { url: '/tools/json_tester' }; },
    naturalDateTest: (string, timezone) => { return { url: `/tools/natural_date_tester?string=${string}&timezone=${timezone}` }; },
    regexTest: () => { return { url: '/tools/regex_tester' }; },
    regexValidate: (regex: string) => { return { url: `/tools/regex_tester/validate?regex=${regex}` }; },
    regexReplaceTest: () => { return { url: '/tools/regex_replace_tester' }; },
    splitAndIndexTest: () => { return { url: '/tools/split_and_index_tester' }; },
    substringTest: () => { return { url: '/tools/substring_tester' }; },
    containsStringTest: () => { return { url: '/tools/contains_string_tester' }; },
    lookupTableTest: () => { return { url: '/tools/lookup_table_tester' }; },
    urlWhitelistCheck: () => { return { url: '/system/urlwhitelist/check' }; },
    urlWhitelistGenerateRegex: () => { return { url: '/system/urlwhitelist/generate_regex' }; },
  },
  UniversalSearchApiController: {
    _streamFilter(streamId: string) {
      return (streamId ? { filter: `streams:${streamId}` } : {});
    },
    _buildBaseQueryString(query: string, timerange: TimeRange, streamId): SearchQueryString {
      const queryString: Partial<SearchQueryString> = {};

      const streamFilter = this._streamFilter(streamId);

      queryString.query = query;

      Object.keys(timerange).forEach((key) => { queryString[key] = timerange[key]; });

      Object.keys(streamFilter).forEach((key) => { queryString[key] = streamFilter[key]; });

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
    changePassword: (userId: string) => { return { url: `/users/${userId}/password` }; },
    create: () => { return { url: '/users' }; },
    list: () => { return { url: '/users' }; },
    paginated: () => { return { url: '/users/paginated' }; },
    load: (userId: string) => { return { url: `/users/id/${userId}` }; },
    loadByUsername: (username: string) => { return { url: `/users/${username}` }; },
    delete: (id: string) => { return { url: `/users/id/${id}` }; },
    update: (userId: string) => { return { url: `/users/${userId}` }; },
    create_token: (userId: string, tokenName: string) => { return { url: `/users/${userId}/tokens/${tokenName}` }; },
    delete_token: (userId: string, tokenName: string) => { return { url: `/users/${userId}/tokens/${tokenName}` }; },
    list_tokens: (userId: string) => { return { url: `/users/${userId}/tokens` }; },
    setStatus: (userId: string, accountStatus) => { return { url: `/users/${userId}/status/${accountStatus}` }; },
  },
  DashboardsController: {
    show: (id) => { return { url: `/dashboards/${id}` }; },
  },
  ExtractorsController: {
    create: (inputId: string) => { return { url: `/system/inputs/${inputId}/extractors` }; },
    delete: (inputId: string, extractorId: string) => { return { url: `/system/inputs/${inputId}/extractors/${extractorId}` }; },
    order: (inputId: string) => { return { url: `/system/inputs/${inputId}/extractors/order` }; },
    update: (inputId: string, extractorId: string) => { return { url: `/system/inputs/${inputId}/extractors/${extractorId}` }; },
  },
  MessagesController: {
    analyze: (index: string, string: string) => { return { url: `/messages/${index}/analyze?string=${string}` }; },
    parse: () => { return { url: '/messages/parse' }; },
    single: (index: string, messageId: string) => { return { url: `/messages/${index}/${messageId}` }; },
    exportSearch: ((searchId: string) => { return { url: `/views/search/messages/${searchId}` }; }),
    exportSearchType: ((searchId: string, searchTypeId: string) => { return { url: `/views/search/messages/${searchId}/${searchTypeId}` }; }),
    jobResults: ((exportJobId: string, filename: string) => { return { url: `/views/search/messages/job/${exportJobId}/${filename}` }; }),
  },
  ExportJobsController: {
    exportSearch: ((searchId: string) => { return { url: `/views/export/${searchId}` }; }),
    exportSearchType: ((searchId: string, searchTypeId: string) => { return { url: `/views/export/${searchId}/${searchTypeId}` }; }),
  },
  MapDataController: {
    search: () => { return { url: '/search/mapdata' }; },
  },
  PipelinesController: {
    list: () => { return { url: '/system/pipelines/pipeline' }; },
    paginatedList: () => { return { url: '/system/pipelines/pipeline/paginated' }; },
    create: () => { return { url: '/system/pipelines/pipeline' }; },
    get: (pipelineId: string) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    update: (pipelineId: string) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    delete: (pipelineId: string) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    parse: () => { return { url: '/system/pipelines/pipeline/parse' }; },
  },
  RulesController: {
    list: () => { return { url: '/system/pipelines/rule' }; },
    paginatedList: () => { return { url: '/system/pipelines/rule/paginated' }; },
    create: () => { return { url: '/system/pipelines/rule' }; },
    get: (ruleId: string) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
    update: (ruleId: string) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
    delete: (ruleId: string) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
    multiple: () => { return { url: '/system/pipelines/rule/multiple' }; },
    functions: () => { return { url: '/system/pipelines/rule/functions' }; },
    parse: () => { return { url: '/system/pipelines/rule/parse' }; },
    metricsConfig: () => { return { url: '/system/pipelines/rule/config/metrics' }; },
  },
  ConnectionsController: {
    list: () => { return { url: '/system/pipelines/connections' }; },
    to_stream: () => { return { url: '/system/pipelines/connections/to_stream' }; },
    to_pipeline: () => { return { url: '/system/pipelines/connections/to_pipeline' }; },
  },
  SimulatorController: {
    simulate: () => { return { url: '/system/pipelines/simulate' }; },
  },
  ping: () => { return { url: '/' }; },
};

export default ApiRoutes;
