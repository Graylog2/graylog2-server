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
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import type { PluginRoute } from 'graylog-web-plugin';

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
  DataNodesPage,
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
  KeyboardShortcutsPage,
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
  SystemLogsPage,
  UserDetailsPage,
  UserCreatePage,
  UserEditPage,
  UserTokensEditPage,
  UsersOverviewPage,
  ViewEventDefinitionPage,
  SidecarFailureTrackingPage,
  IndexSetFieldTypesPage,
} from 'pages';
import AppConfig from 'util/AppConfig';
import { appPrefixed } from 'util/URLUtils';
import App from 'routing/App';
import PageContentLayout from 'components/layout/PageContentLayout';
import RoutePaths from 'routing/Routes';
import RouterErrorBoundary from 'components/errors/RouterErrorBoundary';
import usePluginEntities from 'hooks/usePluginEntities';
import GlobalContextProviders from 'contexts/GlobalContextProviders';

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

  return {
    path: appPrefixed(path),
    element: <WrappedComponent />,
  };
};

const routeHasAppParent = (route: PluginRoute) => route.parentComponent === App;

const AppRouter = () => {
  const pluginRoutes = usePluginEntities('routes');
  const pluginRoutesWithNullParent = pluginRoutes.filter((route) => (route.parentComponent === null)).map((route) => renderPluginRoute({ ...route, parentComponent: GlobalContextProviders }));
  const pluginRoutesWithAppParent = pluginRoutes.filter((route) => routeHasAppParent(route)).map((route) => renderPluginRoute({ ...route, parentComponent: null }));
  const pluginRoutesWithParent = pluginRoutes.filter((route) => (route.parentComponent && !routeHasAppParent(route))).map(renderPluginRoute);
  const standardPluginRoutes = pluginRoutes.filter((route) => (route.parentComponent === undefined)).map(renderPluginRoute);

  const isCloud = AppConfig.isCloud();

  let enableInputsRoute = true;

  if (AppConfig.isCloud()) {
    enableInputsRoute = AppConfig.isFeatureEnabled('cloud_inputs');
  }

  const router = createBrowserRouter([
    ...pluginRoutesWithNullParent,

    {
      path: RoutePaths.STARTPAGE,
      element: <GlobalContextProviders><App /></GlobalContextProviders>,
      children: [
        { path: RoutePaths.STARTPAGE, element: <StartPage /> },
        { path: RoutePaths.SEARCH, element: <DelegatedSearchPage /> },
        ...pluginRoutesWithParent,
        ...pluginRoutesWithAppParent,
        {
          path: `${AppConfig.gl2AppPathPrefix()}/`,
          element: <PageContentLayout />,
          children: [
            { path: RoutePaths.message_show(':index', ':messageId'), element: <ShowMessagePage /> },
            { path: RoutePaths.WELCOME, element: <WelcomePage /> },
            { path: RoutePaths.STREAMS, element: <StreamsPage /> },
            { path: RoutePaths.stream_edit(':streamId'), element: <StreamEditPage /> },
            !isCloud && { path: RoutePaths.stream_outputs(':streamId'), element: <StreamOutputsPage /> },

            { path: RoutePaths.ALERTS.LIST, element: <EventsPage /> },
            { path: RoutePaths.ALERTS.DEFINITIONS.LIST, element: <EventDefinitionsPage /> },
            { path: RoutePaths.ALERTS.DEFINITIONS.CREATE, element: <CreateEventDefinitionPage /> },
            {
              path: RoutePaths.ALERTS.DEFINITIONS.edit(':definitionId'),
              element: <EditEventDefinitionPage />,
            },
            {
              path: RoutePaths.ALERTS.DEFINITIONS.show(':definitionId'),
              element: <ViewEventDefinitionPage />,
            },
            { path: RoutePaths.ALERTS.NOTIFICATIONS.LIST, element: <EventNotificationsPage /> },
            { path: RoutePaths.ALERTS.NOTIFICATIONS.CREATE, element: <CreateEventNotificationPage /> },
            {
              path: RoutePaths.ALERTS.NOTIFICATIONS.edit(':notificationId'),
              element: <EditEventNotificationPage />,
            },
            {
              path: RoutePaths.ALERTS.NOTIFICATIONS.show(':notificationId'),
              element: <ShowEventNotificationPage />,
            },

            enableInputsRoute && { path: RoutePaths.SYSTEM.INPUTS, element: <InputsPage /> },
            !isCloud && { path: RoutePaths.node_inputs(':nodeId'), element: <NodeInputsPage /> },
            !isCloud && { path: RoutePaths.global_input_extractors(':inputId'), element: <ExtractorsPage /> },
            !isCloud && { path: RoutePaths.local_input_extractors(':nodeId', ':inputId'), element: <ExtractorsPage /> },
            !isCloud && { path: RoutePaths.new_extractor(':nodeId', ':inputId'), element: <CreateExtractorsPage /> },
            !isCloud && { path: RoutePaths.edit_extractor(':nodeId', ':inputId', ':extractorId'), element: <EditExtractorsPage /> },
            !isCloud && { path: RoutePaths.import_extractors(':nodeId', ':inputId'), element: <ImportExtractorsPage /> },
            !isCloud && { path: RoutePaths.export_extractors(':nodeId', ':inputId'), element: <ExportExtractorsPage /> },

            { path: `${RoutePaths.SYSTEM.CONFIGURATIONS}/*`, element: <ConfigurationsPage /> },

            { path: RoutePaths.SYSTEM.CONTENTPACKS.LIST, element: <ContentPacksPage /> },
            { path: RoutePaths.SYSTEM.CONTENTPACKS.CREATE, element: <CreateContentPackPage /> },
            {
              path: RoutePaths.SYSTEM.CONTENTPACKS.edit(':contentPackId', ':contentPackRev'),
              element: <EditContentPackPage />,
            },
            { path: RoutePaths.SYSTEM.CONTENTPACKS.show(':contentPackId'), element: <ShowContentPackPage /> },

            { path: RoutePaths.SYSTEM.GROKPATTERNS, element: <GrokPatternsPage /> },

            { path: RoutePaths.SYSTEM.INDEX_SETS.CREATE, element: <IndexSetCreationPage /> },
            { path: RoutePaths.SYSTEM.INDEX_SETS.SHOW(':indexSetId'), element: <IndexSetPage /> },
            { path: RoutePaths.SYSTEM.INDEX_SETS.CONFIGURATION(':indexSetId'), element: <IndexSetConfigurationPage /> },
            { path: RoutePaths.SYSTEM.INDEX_SETS.FIELD_TYPES(':indexSetId'), element: <IndexSetFieldTypesPage /> },

            { path: RoutePaths.SYSTEM.INDICES.LIST, element: <IndicesPage /> },
            !isCloud && (
              { path: RoutePaths.SYSTEM.INDICES.FAILURES, element: <IndexerFailuresPage /> }
            ),

            { path: RoutePaths.SYSTEM.LOOKUPTABLES.OVERVIEW, element: <LUTTablesPage /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.CREATE, element: <LUTTablesPage action="create" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.show(':tableName'), element: <LUTTablesPage action="show" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.edit(':tableName'), element: <LUTTablesPage action="edit" /> },

            { path: RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW, element: <LUTCachesPage /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.CREATE, element: <LUTCachesPage action="create" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.show(':cacheName'), element: <LUTCachesPage action="show" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.CACHES.edit(':cacheName'), element: <LUTCachesPage action="edit" /> },

            { path: RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW, element: <LUTDataAdaptersPage /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE, element: <LUTDataAdaptersPage action="create" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(':adapterName'), element: <LUTDataAdaptersPage action="show" /> },
            { path: RoutePaths.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(':adapterName'), element: <LUTDataAdaptersPage action="edit" /> },

            { path: RoutePaths.SYSTEM.PIPELINES.OVERVIEW, element: <PipelinesOverviewPage /> },
            { path: RoutePaths.SYSTEM.PIPELINES.RULES, element: <RulesPage /> },
            { path: RoutePaths.SYSTEM.PIPELINES.RULE(':ruleId'), element: <RuleDetailsPage /> },
            { path: RoutePaths.SYSTEM.PIPELINES.SIMULATOR, element: <SimulatorPage /> },
            { path: RoutePaths.SYSTEM.PIPELINES.PIPELINE(':pipelineId'), element: <PipelineDetailsPage /> },

            !isCloud && { path: RoutePaths.SYSTEM.LOGGING, element: <LoggersPage /> },
            { path: RoutePaths.SYSTEM.METRICS(':nodeId'), element: <ShowMetricsPage /> },
            !isCloud && { path: RoutePaths.SYSTEM.NODES.LIST, element: <NodesPage /> },
            !isCloud && { path: RoutePaths.SYSTEM.NODES.SHOW(':nodeId'), element: <ShowNodePage /> },
            !isCloud && { path: RoutePaths.SYSTEM.DATANODES.OVERVIEW, element: <DataNodesPage /> },
            !isCloud && { path: RoutePaths.SYSTEM.OUTPUTS, element: <SystemOutputsPage /> },

            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE, element: <AuthenticationPage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.CREATE, element: <AuthenticationCreatePage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW, element: <AuthenticationOverviewPage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.show(':backendId'), element: <AuthenticationBackendDetailsPage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.edit(':backendId'), element: <AuthenticationBackendEditPage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(':name'), element: <AuthenticationBackendCreatePage /> }
            ),

            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.AUTHENTICATORS.SHOW, element: <AuthenticatorsPage /> }
            ),
            !isCloud && (
              { path: RoutePaths.SYSTEM.AUTHENTICATION.AUTHENTICATORS.EDIT, element: <AuthenticatorsEditPage /> }
            ),

            { path: RoutePaths.SYSTEM.USERS.OVERVIEW, element: <UsersOverviewPage /> },
            { path: RoutePaths.SYSTEM.USERS.CREATE, element: <UserCreatePage /> },
            { path: RoutePaths.SYSTEM.USERS.show(':userId'), element: <UserDetailsPage /> },
            { path: RoutePaths.SYSTEM.USERS.edit(':userId'), element: <UserEditPage /> },
            { path: RoutePaths.SYSTEM.USERS.TOKENS.edit(':userId'), element: <UserTokensEditPage /> },

            { path: RoutePaths.SYSTEM.AUTHZROLES.OVERVIEW, element: <RolesOverviewPage /> },
            { path: RoutePaths.SYSTEM.AUTHZROLES.show(':roleId'), element: <RoleDetailsPage /> },
            { path: RoutePaths.SYSTEM.AUTHZROLES.edit(':roleId'), element: <RoleEditPage /> },

            { path: RoutePaths.SYSTEM.OVERVIEW, element: <SystemOverviewPage /> },
            { path: RoutePaths.SYSTEM.PROCESSBUFFERDUMP(':nodeId'), element: <ProcessBufferDumpPage /> },
            { path: RoutePaths.SYSTEM.THREADDUMP(':nodeId'), element: <ThreadDumpPage /> },
            { path: RoutePaths.SYSTEM.SYSTEMLOGS(':nodeId'), element: <SystemLogsPage /> },
            { path: RoutePaths.SYSTEM.ENTERPRISE, element: <EnterprisePage /> },

            { path: RoutePaths.SYSTEM.SIDECARS.OVERVIEW, element: <SidecarsPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.STATUS(':sidecarId'), element: <SidecarStatusPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.ADMINISTRATION, element: <SidecarAdministrationPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.CONFIGURATION, element: <SidecarConfigurationPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.FAILURE_TRACKING, element: <SidecarFailureTrackingPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.NEW_CONFIGURATION, element: <SidecarNewConfigurationPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.EDIT_CONFIGURATION(':configurationId'), element: <SidecarEditConfigurationPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.NEW_COLLECTOR, element: <SidecarNewCollectorPage /> },
            { path: RoutePaths.SYSTEM.SIDECARS.EDIT_COLLECTOR(':collectorId'), element: <SidecarEditCollectorPage /> },
            { path: RoutePaths.KEYBOARD_SHORTCUTS, element: <KeyboardShortcutsPage /> },
            ...standardPluginRoutes,
            { path: '*', element: <NotFoundPage displayPageLayout={false} /> },
          ].filter((route) => !!route),
        },
        { path: RoutePaths.NOTFOUND, element: <NotFoundPage /> },
      ],
    },
  ]);

  return (
    <RouterErrorBoundary>
      <RouterProvider router={router} />
    </RouterErrorBoundary>
  );
};

export default AppRouter;
