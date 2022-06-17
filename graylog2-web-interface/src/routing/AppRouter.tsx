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
import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import type { PluginRoute } from 'graylog-web-plugin';

import App from 'routing/App';
import PageContentLayout from 'components/layout/PageContentLayout';
import AppConfig from 'util/AppConfig';
import RoutePaths from 'routing/Routes';
import { appPrefixed } from 'util/URLUtils';
import {
  AuthenticationCreatePage,
  AuthenticationBackendCreatePage,
  AuthenticationBackendDetailsPage,
  AuthenticationBackendEditPage,
  AuthenticationOverviewPage,
  AuthenticationPage,
  AuthenticatorsPage,
  AuthenticatorsEditPage,
  ConfigurationsPage,
  ContentPacksPage,
  CreateContentPackPage,
  CreateEventDefinitionPage,
  CreateEventNotificationPage,
  CreateExtractorsPage,
  DelegatedSearchPage,
  EditEventDefinitionPage,
  EditEventNotificationPage,
  EditContentPackPage,
  EditExtractorsPage,
  EnterprisePage,
  EventDefinitionsPage,
  EventNotificationsPage,
  EventsPage,
  ExportExtractorsPage,
  ExtractorsPage,
  WelcomePage,
  GrokPatternsPage,
  ImportExtractorsPage,
  IndexerFailuresPage,
  IndexSetConfigurationPage,
  IndexSetCreationPage,
  IndexSetPage,
  IndicesPage,
  InputsPage,
  LoggersPage,
  LUTCachesPage,
  LUTDataAdaptersPage,
  LUTTablesPage,
  NodeInputsPage,
  NodesPage,
  NotFoundPage,
  PipelineDetailsPage,
  PipelinesOverviewPage,
  ProcessBufferDumpPage,
  RoleDetailsPage,
  RoleEditPage,
  RolesOverviewPage,
  RuleDetailsPage,
  RulesPage,
  SecurityPage,
  ShowContentPackPage,
  ShowEventNotificationPage,
  ShowMessagePage,
  ShowMetricsPage,
  ShowNodePage,
  SidecarAdministrationPage,
  SidecarConfigurationPage,
  SidecarEditCollectorPage,
  SidecarEditConfigurationPage,
  SidecarNewCollectorPage,
  SidecarNewConfigurationPage,
  SidecarsPage,
  SidecarStatusPage,
  SimulatorPage,
  StartPage,
  StreamEditPage,
  StreamOutputsPage,
  StreamsPage,
  SystemOutputsPage,
  SystemOverviewPage,
  ThreadDumpPage,
  UserDetailsPage,
  UserCreatePage,
  UserEditPage,
  UserTokensEditPage,
  UsersOverviewPage,
  ViewEventDefinitionPage,
  SidecarFailureTrackingPage,
} from 'pages';
import RouterErrorBoundary from 'components/errors/RouterErrorBoundary';
import usePluginEntities from 'hooks/usePluginEntities';

const renderPluginRoute = ({ path, component: Component, parentComponent, requiredFeatureFlag }: PluginRoute) => {
  if (requiredFeatureFlag && !AppConfig.isFeatureEnabled(requiredFeatureFlag)) {
    return null;
  }

  const ParentComponent = parentComponent ?? React.Fragment;
  const WrappedComponent = () => (
    <ParentComponent>
      <Component />
    </ParentComponent>
  );

  return (
    <Route key={`${path}-${Component.displayName}`}
           path={appPrefixed(path)}
           element={<WrappedComponent />} />
  );
};

const routeHasAppParent = (route: PluginRoute) => route.parentComponent === App;

const AppRouter = () => {
  const pluginRoutes = usePluginEntities('routes');
  const pluginRoutesWithNullParent = pluginRoutes.filter((route) => (route.parentComponent === null)).map(renderPluginRoute);
  const pluginRoutesWithAppParent = pluginRoutes.filter((route) => routeHasAppParent(route)).map((route) => renderPluginRoute({ ...route, parentComponent: null }));
  const pluginRoutesWithParent = pluginRoutes.filter((route) => (route.parentComponent && !routeHasAppParent(route))).map(renderPluginRoute);
  const standardPluginRoutes = pluginRoutes.filter((route) => (route.parentComponent === undefined)).map(renderPluginRoute);

  const isCloud = AppConfig.isCloud();

  return (
    <BrowserRouter>
      <RouterErrorBoundary>
        <Routes>
          {pluginRoutesWithNullParent}

          <Route path={RoutePaths.STARTPAGE} element={<App />}>
            <Route index element={<StartPage />} />
            <Route path={RoutePaths.SEARCH} element={<DelegatedSearchPage />} />
            {pluginRoutesWithParent}
            {pluginRoutesWithAppParent}
            <Route path="/" element={<PageContentLayout />}>
              <Route path={RoutePaths.message_show(':index', ':messageId')} element={<ShowMessagePage />} />
              <Route path={RoutePaths.GETTING_STARTED} element={<GettingStartedPage />} />
              <Route path={RoutePaths.STREAMS} element={<StreamsPage />} />
              <Route path={RoutePaths.stream_edit(':streamId')} element={<StreamEditPage />} />
              {!isCloud && <Route path={RoutePaths.stream_outputs(':streamId')} element={<StreamOutputsPage />} />}

              <Route path={RoutePaths.ALERTS.LIST} element={<EventsPage />} />
              <Route path={RoutePaths.ALERTS.DEFINITIONS.LIST} element={<EventDefinitionsPage />} />
              <Route path={RoutePaths.ALERTS.DEFINITIONS.CREATE} element={<CreateEventDefinitionPage />} />
              <Route path={RoutePaths.ALERTS.DEFINITIONS.edit(':definitionId')}
                     element={<EditEventDefinitionPage />} />
              <Route path={RoutePaths.ALERTS.DEFINITIONS.show(':definitionId')}
                     element={<ViewEventDefinitionPage />} />
              <Route path={RoutePaths.ALERTS.NOTIFICATIONS.LIST} element={<EventNotificationsPage />} />
              <Route path={RoutePaths.ALERTS.NOTIFICATIONS.CREATE} element={<CreateEventNotificationPage />} />
              <Route path={RoutePaths.ALERTS.NOTIFICATIONS.edit(':notificationId')}
                     element={<EditEventNotificationPage />} />
              <Route path={RoutePaths.ALERTS.NOTIFICATIONS.show(':notificationId')}
                     element={<ShowEventNotificationPage />} />

                      <Route path={RoutePaths.SYSTEM.INPUTS} element={<InputsPage />} />
              {!isCloud && <Route path={RoutePaths.node_inputs(':nodeId')} element={<NodeInputsPage />} />}
              {!isCloud && (
                <Route path={RoutePaths.global_input_extractors(':inputId')} element={<ExtractorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.local_input_extractors(':nodeId', ':inputId')}
                       element={<ExtractorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.new_extractor(':nodeId', ':inputId')}
                       element={<CreateExtractorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.edit_extractor(':nodeId', ':inputId', ':extractorId')}
                       element={<EditExtractorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.import_extractors(':nodeId', ':inputId')}
                       element={<ImportExtractorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.export_extractors(':nodeId', ':inputId')}
                       element={<ExportExtractorsPage />} />
              )}

              <Route path={RoutePaths.SYSTEM.CONFIGURATIONS} element={<ConfigurationsPage />} />

              <Route path={RoutePaths.SYSTEM.CONTENTPACKS.LIST} element={<ContentPacksPage />} />
              <Route path={RoutePaths.SYSTEM.CONTENTPACKS.CREATE} element={<CreateContentPackPage />} />
              <Route path={RoutePaths.SYSTEM.CONTENTPACKS.edit(':contentPackId', ':contentPackRev')}
                     element={<EditContentPackPage />} />
              <Route path={RoutePaths.SYSTEM.CONTENTPACKS.show(':contentPackId')}
                     element={<ShowContentPackPage />} />

              <Route path={RoutePaths.SYSTEM.GROKPATTERNS} element={<GrokPatternsPage />} />

              <Route path={RoutePaths.SYSTEM.INDEX_SETS.CREATE} element={<IndexSetCreationPage />} />
              <Route path={RoutePaths.SYSTEM.INDEX_SETS.SHOW(':indexSetId')} element={<IndexSetPage />} />
              <Route path={RoutePaths.SYSTEM.INDEX_SETS.CONFIGURATION(':indexSetId')}
                     element={<IndexSetConfigurationPage />} />

              <Route path={RoutePaths.SYSTEM.INDICES.LIST} element={<IndicesPage />} />
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.INDICES.FAILURES} element={<IndexerFailuresPage />} />
              )}

              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.OVERVIEW} element={<LUTTablesPage />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.CREATE}
                     element={<LUTTablesPage action="create" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.show(':tableName')}
                     element={<LUTTablesPage action="show" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.edit(':tableName')}
                     element={<LUTTablesPage action="edit" />} />

              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} element={<LUTCachesPage />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.CREATE}
                     element={<LUTCachesPage action="create" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.show(':cacheName')}
                     element={<LUTCachesPage action="show" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.edit(':cacheName')}
                     element={<LUTCachesPage action="edit" />} />

              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}
                     element={<LUTDataAdaptersPage />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}
                     element={<LUTDataAdaptersPage action="create" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(':adapterName')}
                     element={<LUTDataAdaptersPage action="show" />} />
              <Route path={RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(':adapterName')}
                     element={<LUTDataAdaptersPage action="edit" />} />

              <Route path={RoutePaths.SYSTEM.PIPELINES.OVERVIEW} element={<PipelinesOverviewPage />} />
              <Route path={RoutePaths.SYSTEM.PIPELINES.RULES} element={<RulesPage />} />
              <Route path={RoutePaths.SYSTEM.PIPELINES.RULE(':ruleId')} element={<RuleDetailsPage />} />
              <Route path={RoutePaths.SYSTEM.PIPELINES.SIMULATOR} element={<SimulatorPage />} />
              <Route path={RoutePaths.SYSTEM.PIPELINES.PIPELINE(':pipelineId')} element={<PipelineDetailsPage />} />

              {!isCloud && <Route path={RoutePaths.SYSTEM.LOGGING} element={<LoggersPage />} />}
              <Route path={RoutePaths.SYSTEM.METRICS(':nodeId')} element={<ShowMetricsPage />} />
              {!isCloud && <Route path={RoutePaths.SYSTEM.NODES.LIST} element={<NodesPage />} />}
              {!isCloud && <Route path={RoutePaths.SYSTEM.NODES.SHOW(':nodeId')} element={<ShowNodePage />} />}

              {!isCloud && <Route path={RoutePaths.SYSTEM.OUTPUTS} element={<SystemOutputsPage />} />}

              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE}
                       element={<AuthenticationPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.CREATE}
                       element={<AuthenticationCreatePage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW}
                       element={<AuthenticationOverviewPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.show(':backendId')}
                       element={<AuthenticationBackendDetailsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.edit(':backendId')}
                       element={<AuthenticationBackendEditPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(':name')}
                       element={<AuthenticationBackendCreatePage />} />
              )}

              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.AUTHENTICATORS.SHOW}
                       element={<AuthenticatorsPage />} />
              )}
              {!isCloud && (
                <Route path={RoutePaths.SYSTEM.AUTHENTICATION.AUTHENTICATORS.EDIT}
                       element={<AuthenticatorsEditPage />} />
              )}

              <Route path={RoutePaths.SYSTEM.USERS.OVERVIEW} element={<UsersOverviewPage />} />
              <Route path={RoutePaths.SYSTEM.USERS.CREATE} element={<UserCreatePage />} />
              <Route path={RoutePaths.SYSTEM.USERS.show(':userId')} element={<UserDetailsPage />} />
              <Route path={RoutePaths.SYSTEM.USERS.edit(':userId')} element={<UserEditPage />} />
              <Route path={RoutePaths.SYSTEM.USERS.TOKENS.edit(':userId')} element={<UserTokensEditPage />} />

              <Route path={RoutePaths.SYSTEM.AUTHZROLES.OVERVIEW} element={<RolesOverviewPage />} />
              <Route path={RoutePaths.SYSTEM.AUTHZROLES.show(':roleId')} element={<RoleDetailsPage />} />
              <Route path={RoutePaths.SYSTEM.AUTHZROLES.edit(':roleId')} element={<RoleEditPage />} />

              <Route path={RoutePaths.SYSTEM.OVERVIEW} element={<SystemOverviewPage />} />
              <Route path={RoutePaths.SYSTEM.PROCESSBUFFERDUMP(':nodeId')} element={<ProcessBufferDumpPage />} />
              <Route path={RoutePaths.SYSTEM.THREADDUMP(':nodeId')} element={<ThreadDumpPage />} />
              <Route path={RoutePaths.SYSTEM.ENTERPRISE} element={<EnterprisePage />} />
              <Route path={RoutePaths.SECURITY} element={<SecurityPage />} />

              <Route path={RoutePaths.SYSTEM.SIDECARS.OVERVIEW} element={<SidecarsPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.STATUS(':sidecarId')}
                     element={<SidecarStatusPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.ADMINISTRATION}
                     element={<SidecarAdministrationPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.CONFIGURATION}
                     element={<SidecarConfigurationPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.FAILURE_TRACKING}
                     element={>SidecarFailureTrackingPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.NEW_CONFIGURATION}
                     element={<SidecarNewConfigurationPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.EDIT_CONFIGURATION(':configurationId')}
                     element={<SidecarEditConfigurationPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.NEW_COLLECTOR}
                     element={<SidecarNewCollectorPage />} />
              <Route path={RoutePaths.SYSTEM.SIDECARS.EDIT_COLLECTOR(':collectorId')}
                     element={<SidecarEditCollectorPage />} />
              {standardPluginRoutes}
              <Route path="*" element={<NotFoundPage displayPageLayout={false} />} />
            </Route>
            <Route path={RoutePaths.NOTFOUND} element={<NotFoundPage />} />
          </Route>
        </Routes>
      </RouterErrorBoundary>
    </BrowserRouter>
  );
};

export default AppRouter;
