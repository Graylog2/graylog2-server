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

const ApiRoutes = {
  AlarmCallbacksApiController: {
    available: () => { return { url: '/alerts/callbacks/types' }; },
    create: (streamId) => { return { url: `/streams/${streamId}/alarmcallbacks` }; },
    delete: (streamId, alarmCallbackId) => { return { url: `/streams/${streamId}/alarmcallbacks/${alarmCallbackId}` }; },
    listAll: () => { return { url: '/alerts/callbacks' }; },
    list: (streamId) => { return { url: `/streams/${streamId}/alarmcallbacks` }; },
    testAlert: (alarmCallbackId) => { return { url: `/alerts/callbacks/${alarmCallbackId}/test` }; },
    update: (streamId, alarmCallbackId) => { return { url: `/streams/${streamId}/alarmcallbacks/${alarmCallbackId}` }; },
  },
  AlarmCallbackHistoryApiController: {
    list: (streamId, alertId) => { return { url: `/streams/${streamId}/alerts/${alertId}/history` }; },
  },
  AlertsApiController: {
    get: (alertId) => { return { url: `/streams/alerts/${alertId}` }; },
    list: (streamId, since) => { return { url: `/streams/${streamId}/alerts?since=${since}` }; },
    listPaginated: (streamId, skip, limit, state) => { return { url: `/streams/${streamId}/alerts/paginated?skip=${skip}&limit=${limit}&state=${state}` }; },
    listAllPaginated: (skip, limit, state) => { return { url: `/streams/alerts/paginated?skip=${skip}&limit=${limit}&state=${state}` }; },
    listAllStreams: (since) => { return { url: `/streams/alerts?since=${since}` }; },
  },
  AlertConditionsApiController: {
    available: () => { return { url: '/alerts/conditions/types' }; },
    list: () => { return { url: '/alerts/conditions' }; },
  },
  AuthenticationController: {
    create: () => ({ url: '/system/authentication/services/backends' }),
    delete: (backendId) => ({ url: `/system/authentication/services/backends/${backendId}` }),
    disableUser: (userId) => ({ url: `/system/authentication/users/${userId}/disable` }),
    enableUser: (userId) => ({ url: `/system/authentication/users/${userId}/enable` }),
    load: (serviceId) => ({ url: `/system/authentication/services/backends/${serviceId}` }),
    loadActive: () => ({ url: '/system/authentication/services/active-backend' }),
    loadUsersPaginated: (authBackendId) => ({ url: `/system/authentication/services/backends/${authBackendId}/users` }),
    servicesPaginated: () => ({ url: '/system/authentication/services/backends' }),
    testConnection: () => ({ url: '/system/authentication/services/test/backend/connection' }),
    testLogin: () => ({ url: '/system/authentication/services/test/backend/login' }),
    update: (serviceId) => ({ url: `/system/authentication/services/backends/${serviceId}` }),
    updateConfiguration: () => ({ url: '/system/authentication/services/configuration' }),
  },
  AuthzRolesController: {
    load: (roleId) => { return { url: `/authz/roles/${roleId}` }; },
    delete: (roleId) => { return { url: `/authz/roles/${roleId}` }; },
    list: () => { return { url: '/authz/roles' }; },
    removeMember: (roleId, username) => { return { url: `/authz/roles/${roleId}/assignee/${username}` }; },
    addMembers: (roleId) => { return { url: `/authz/roles/${roleId}/assignees` }; },
    loadRolesForUser: (username) => { return { url: `/authz/roles/user/${username}` }; },
    loadUsersForRole: (roleId) => { return { url: `/authz/roles/${roleId}/assignees` }; },
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
    get: (contentPackId) => { return { url: `/system/content_packs/${contentPackId}` }; },
    getRev: (contentPackId, revision) => { return { url: `/system/content_packs/${contentPackId}/${revision}` }; },
    downloadRev: (contentPackId, revision) => { return { url: `/system/content_packs/${contentPackId}/${revision}/download` }; },
    create: () => { return { url: '/system/content_packs' }; },
    delete: (contentPackId) => { return { url: `/system/content_packs/${contentPackId}` }; },
    deleteRev: (contentPackId, revision) => { return { url: `/system/content_packs/${contentPackId}/${revision}` }; },
    install: (contentPackId, revision) => { return { url: `/system/content_packs/${contentPackId}/${revision}/installations` }; },
    installList: (contentPackId) => { return { url: `/system/content_packs/${contentPackId}/installations` }; },
    uninstall: (contentPackId, installId) => { return { url: `/system/content_packs/${contentPackId}/installations/${installId}` }; },
    uninstallDetails: (contentPackId, installId) => { return { url: `/system/content_packs/${contentPackId}/installations/${installId}/uninstall_details` }; },
  },
  CountsApiController: {
    total: () => { return { url: '/count/total' }; },
    indexSetTotal: (indexSetId) => { return { url: `/count/${indexSetId}/total` }; },
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
    removeWidget: (dashboardId, widgetId) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    widget: (dashboardId, widgetId) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    updateWidget: (dashboardId, widgetId) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}` }; },
    widgetValue: (dashboardId, widgetId) => { return { url: `/dashboards/${dashboardId}/widgets/${widgetId}/value` }; },
    updatePositions: (dashboardId) => { return { url: `/dashboards/${dashboardId}/positions` }; },
  },
  DecoratorsResource: {
    available: () => { return { url: '/search/decorators/available' }; },
    create: () => { return { url: '/search/decorators' }; },
    get: () => { return { url: '/search/decorators' }; },
    remove: (decoratorId) => { return { url: `/search/decorators/${decoratorId}` }; },
    update: (decoratorId) => { return { url: `/search/decorators/${decoratorId}` }; },
  },
  DeflectorApiController: {
    cycle: (indexSetId) => { return { url: `/cluster/deflector/${indexSetId}/cycle` }; },
    list: (indexSetId) => { return { url: `/system/deflector/${indexSetId}` }; },
  },
  EntityShareController: {
    prepare: (entityGRN) => { return { url: `/authz/shares/entities/${entityGRN}/prepare` }; },
    update: (entityGRN) => { return { url: `/authz/shares/entities/${entityGRN}` }; },
    userSharesPaginated: (username) => { return { url: `/authz/shares/user/${username}` }; },
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
    count: (since) => { return { url: `/system/indexer/failures/count?since=${since}` }; },
    list: (limit, offset) => { return { url: `/system/indexer/failures?limit=${limit}&offset=${offset}` }; },
  },
  IndexerOverviewApiResource: {
    list: (indexSetId) => { return { url: `/system/indexer/overview/${indexSetId}` }; },
  },
  IndexRangesApiController: {
    list: () => { return { url: '/system/indices/ranges' }; },
    rebuild: (indexSetId) => { return { url: `/system/indices/ranges/index_set/${indexSetId}/rebuild` }; },
    rebuildSingle: (index) => { return { url: `/system/indices/ranges/${index}/rebuild` }; },
  },
  IndexSetsApiController: {
    list: (stats) => { return { url: `/system/indices/index_sets?stats=${stats}` }; },
    listPaginated: (skip, limit, stats) => { return { url: `/system/indices/index_sets?skip=${skip}&limit=${limit}&stats=${stats}` }; },
    get: (indexSetId) => { return { url: `/system/indices/index_sets/${indexSetId}` }; },
    create: () => { return { url: '/system/indices/index_sets' }; },
    delete: (indexSetId, deleteIndices) => { return { url: `/system/indices/index_sets/${indexSetId}?delete_indices=${deleteIndices}` }; },
    setDefault: (indexSetId) => { return { url: `/system/indices/index_sets/${indexSetId}/default` }; },
    stats: () => { return { url: '/system/indices/index_sets/stats' }; },
  },
  IndicesApiController: {
    close: (indexName) => { return { url: `/system/indexer/indices/${indexName}/close` }; },
    delete: (indexName) => { return { url: `/system/indexer/indices/${indexName}` }; },
    list: (indexSetId) => { return { url: `/system/indexer/indices/${indexSetId}/list` }; },
    listAll: () => { return { url: '/system/indexer/indices' }; },
    listClosed: (indexSetId) => { return { url: `/system/indexer/indices/${indexSetId}/closed` }; },
    multiple: () => { return { url: '/system/indexer/indices/multiple' }; },
    reopen: (indexName) => { return { url: `/system/indexer/indices/${indexName}/reopen` }; },
  },
  InputsApiController: {
    list: () => { return { url: '/system/inputs' }; },
    get: (id) => { return { url: `/system/inputs/${id}` }; },
    globalRecentMessage: (inputId) => { return { url: `/${inputId}` }; },
  },
  InputStatesController: {
    start: (inputId) => { return { url: `/system/inputstates/${inputId}` }; },
    stop: (inputId) => { return { url: `/system/inputstates/${inputId}` }; },
  },
  ClusterInputStatesController: {
    list: () => { return { url: '/cluster/inputstates' }; },
    start: (inputId) => { return { url: `/cluster/inputstates/${inputId}` }; },
    stop: (inputId) => { return { url: `/cluster/inputstates/${inputId}` }; },
  },
  ClusterLoggersResource: {
    loggers: () => { return { url: '/cluster/system/loggers' }; },
    subsystems: () => { return { url: '/cluster/system/loggers/subsystems' }; },
    setSubsystemLoggerLevel: (nodeId, subsystem, loglevel) => { return { url: `/cluster/system/loggers/${nodeId}/subsystems/${subsystem}/level/${loglevel}` }; },
  },
  MessageFieldsApiController: {
    list: () => { return { url: '/system/fields' }; },
  },
  MetricsApiController: {
    multiple: () => { return { url: '/system/metrics/multiple' }; },
    byNamespace: (namespace) => { return { url: `/system/metrics/namespace/${namespace}` }; },
  },
  ClusterMetricsApiController: {
    multiple: (nodeId) => { return { url: `/cluster/${nodeId}/metrics/multiple` }; },
    multipleAllNodes: () => { return { url: '/cluster/metrics/multiple' }; },
    byNamespace: (nodeId, namespace) => { return { url: `/cluster/${nodeId}/metrics/namespace/${namespace}` }; },
  },
  NotificationsApiController: {
    delete: (type) => { return { url: `/system/notifications/${type}` }; },
    list: () => { return { url: '/system/notifications' }; },
  },
  OutputsApiController: {
    index: () => { return { url: '/system/outputs' }; },
    create: () => { return { url: '/system/outputs' }; },
    delete: (outputId) => { return { url: `/system/outputs/${outputId}` }; },
    update: (outputId) => { return { url: `/system/outputs/${outputId}` }; },
    availableType: (type) => { return { url: `/system/outputs/available/${type}` }; },
    availableTypes: () => { return { url: '/system/outputs/available' }; },
  },
  RolesApiController: {
    listRoles: () => { return { url: '/roles' }; },
    createRole: () => { return { url: '/roles' }; },
    updateRole: (rolename) => { return { url: `/roles/${rolename}` }; },
    deleteRole: (rolename) => { return { url: `/roles/${rolename}` }; },
    loadMembers: (rolename) => { return { url: `/roles/${rolename}/members` }; },
  },
  SavedSearchesApiController: {
    create: () => { return { url: '/search/saved' }; },
    delete: (savedSearchId) => { return { url: `/search/saved/${savedSearchId}` }; },
    update: (savedSearchId) => { return { url: `/search/saved/${savedSearchId}` }; },
  },
  SessionsApiController: {
    validate: () => { return { url: '/system/sessions' }; },
  },
  StreamAlertsApiController: {
    create: (streamId) => { return { url: `/streams/${streamId}/alerts/conditions` }; },
    delete: (streamId, alertConditionId) => { return { url: `/streams/${streamId}/alerts/conditions/${alertConditionId}` }; },
    get: (streamId, conditionId) => { return { url: `/streams/${streamId}/alerts/conditions/${conditionId}` }; },
    list: (streamId) => { return { url: `/streams/${streamId}/alerts/conditions` }; },
    update: (streamId, alertConditionId) => { return { url: `/streams/${streamId}/alerts/conditions/${alertConditionId}` }; },
    sendDummyAlert: (streamId) => { return { url: `/streams/${streamId}/alerts/sendDummyAlert` }; },
    test: (streamId, conditionId) => { return { url: `/streams/${streamId}/alerts/conditions/${conditionId}/test` }; },
  },
  StreamsApiController: {
    index: () => { return { url: '/streams' }; },
    paginated: () => { return { url: '/streams/paginated' }; },
    get: (streamId) => { return { url: `/streams/${streamId}` }; },
    create: () => { return { url: '/streams' }; },
    update: (streamId) => { return { url: `/streams/${streamId}` }; },
    cloneStream: (streamId) => { return { url: `/streams/${streamId}/clone` }; },
    delete: (streamId) => { return { url: `/streams/${streamId}` }; },
    pause: (streamId) => { return { url: `/streams/${streamId}/pause` }; },
    resume: (streamId) => { return { url: `/streams/${streamId}/resume` }; },
    testMatch: (streamId) => { return { url: `/streams/${streamId}/testMatch` }; },
  },
  StreamOutputsApiController: {
    add: (streamId) => { return { url: `/streams/${streamId}/outputs` }; },
    index: (streamId) => { return { url: `/streams/${streamId}/outputs` }; },
    delete: (streamId, outputId) => { return { url: `/streams/${streamId}/outputs/${outputId}` }; },
  },
  StreamRulesApiController: {
    delete: (streamId, streamRuleId) => { return { url: `/streams/${streamId}/rules/${streamRuleId}` }; },
    update: (streamId, streamRuleId) => { return { url: `/streams/${streamId}/rules/${streamRuleId}` }; },
    create: (streamId) => { return { url: `/streams/${streamId}/rules` }; },
  },
  SystemApiController: {
    info: () => { return { url: '/system' }; },
    jvm: () => { return { url: '/system/jvm' }; },
    fields: () => { return { url: '/system/fields' }; },
    locales: () => { return { url: '/system/locales' }; },
  },
  SystemJobsApiController: {
    list: () => { return { url: '/cluster/jobs' }; },
    getJob: (jobId) => { return { url: `/cluster/jobs/${jobId}` }; },
    cancelJob: (jobId) => { return { url: `/cluster/jobs/${jobId}` }; },
  },
  SystemMessagesApiController: {
    all: (page) => { return { url: `/system/messages?page=${page}` }; },
  },
  ToolsApiController: {
    grokTest: () => { return { url: '/tools/grok_tester' }; },
    jsonTest: () => { return { url: '/tools/json_tester' }; },
    naturalDateTest: (text) => { return { url: `/tools/natural_date_tester?string=${text}` }; },
    regexTest: () => { return { url: '/tools/regex_tester' }; },
    regexValidate: (regex) => { return { url: `/tools/regex_tester/validate?regex=${regex}` }; },
    regexReplaceTest: () => { return { url: '/tools/regex_replace_tester' }; },
    splitAndIndexTest: () => { return { url: '/tools/split_and_index_tester' }; },
    substringTest: () => { return { url: '/tools/substring_tester' }; },
    containsStringTest: () => { return { url: '/tools/contains_string_tester' }; },
    lookupTableTest: () => { return { url: '/tools/lookup_table_tester' }; },
    urlWhitelistCheck: () => { return { url: '/system/urlwhitelist/check' }; },
    urlWhitelistGenerateRegex: () => { return { url: '/system/urlwhitelist/generate_regex' }; },
  },
  UniversalSearchApiController: {
    _streamFilter(streamId) {
      return (streamId ? { filter: `streams:${streamId}` } : {});
    },
    _buildBaseQueryString(query, timerange, streamId) {
      const queryString = {};

      const streamFilter = this._streamFilter(streamId);

      queryString.query = query;

      Object.keys(timerange).forEach((key) => { queryString[key] = timerange[key]; });

      Object.keys(streamFilter).forEach((key) => { queryString[key] = streamFilter[key]; });

      return queryString;
    },
    _buildUrl(url, queryString) {
      return `${url}?${Qs.stringify(queryString)}`;
    },
    search(type, query, timerange, streamId, limit, offset, sortField, sortOrder, decorate) {
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
    export(type, query, timerange, streamId, limit, offset, fields) {
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
    changePassword: (userId) => { return { url: `/users/${userId}/password` }; },
    create: () => { return { url: '/users' }; },
    list: () => { return { url: '/users' }; },
    paginated: () => { return { url: '/users/paginated' }; },
    load: (userId) => { return { url: `/users/id/${userId}` }; },
    loadByUsername: (username) => { return { url: `/users/${username}` }; },
    delete: (id) => { return { url: `/users/id/${id}` }; },
    update: (userId) => { return { url: `/users/${userId}` }; },
    create_token: (userId, tokenName) => { return { url: `/users/${userId}/tokens/${tokenName}` }; },
    delete_token: (userId, tokenName) => { return { url: `/users/${userId}/tokens/${tokenName}` }; },
    list_tokens: (userId) => { return { url: `/users/${userId}/tokens` }; },
    setStatus: (userId, accountStatus) => { return { url: `/users/${userId}/status/${accountStatus}` }; },
  },
  DashboardsController: {
    show: (id) => { return { url: `/dashboards/${id}` }; },
  },
  ExtractorsController: {
    create: (inputId) => { return { url: `/system/inputs/${inputId}/extractors` }; },
    delete: (inputId, extractorId) => { return { url: `/system/inputs/${inputId}/extractors/${extractorId}` }; },
    order: (inputId) => { return { url: `/system/inputs/${inputId}/extractors/order` }; },
    update: (inputId, extractorId) => { return { url: `/system/inputs/${inputId}/extractors/${extractorId}` }; },
  },
  MessagesController: {
    analyze: (index, string) => { return { url: `/messages/${index}/analyze?string=${string}` }; },
    parse: () => { return { url: '/messages/parse' }; },
    single: (index, messageId) => { return { url: `/messages/${index}/${messageId}` }; },
    exportSearch: ((searchId) => { return { url: `/views/search/messages/${searchId}` }; }),
    exportSearchType: ((searchId, searchTypeId) => { return { url: `/views/search/messages/${searchId}/${searchTypeId}` }; }),
    jobResults: ((exportJobId, filename) => { return { url: `/views/search/messages/job/${exportJobId}/${filename}` }; }),
  },
  ExportJobsController: {
    exportSearch: ((searchId) => { return { url: `/views/export/${searchId}` }; }),
    exportSearchType: ((searchId, searchTypeId) => { return { url: `/views/export/${searchId}/${searchTypeId}` }; }),
  },
  MapDataController: {
    search: () => { return { url: '/search/mapdata' }; },
  },
  PipelinesController: {
    list: () => { return { url: '/system/pipelines/pipeline' }; },
    create: () => { return { url: '/system/pipelines/pipeline' }; },
    get: (pipelineId) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    update: (pipelineId) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    delete: (pipelineId) => { return { url: `/system/pipelines/pipeline/${pipelineId}` }; },
    parse: () => { return { url: '/system/pipelines/pipeline/parse' }; },
  },
  RulesController: {
    list: () => { return { url: '/system/pipelines/rule' }; },
    create: () => { return { url: '/system/pipelines/rule' }; },
    get: (ruleId) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
    update: (ruleId) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
    delete: (ruleId) => { return { url: `/system/pipelines/rule/${ruleId}` }; },
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
