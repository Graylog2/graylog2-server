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
import loadAsync from 'routing/loadAsync';

const AuthenticationBackendCreatePage = loadAsync(() => import('./AuthenticationBackendCreatePage'));
const AuthenticationBackendDetailsPage = loadAsync(() => import('./AuthenticationBackendDetailsPage'));
const AuthenticationBackendEditPage = loadAsync(() => import('./AuthenticationBackendEditPage'));
const AuthenticationCreatePage = loadAsync(() => import('./AuthenticationCreatePage'));
const AuthenticationOverviewPage = loadAsync(() => import('./AuthenticationOverviewPage'));
const AuthenticationPage = loadAsync(() => import('./AuthenticationPage'));
const AuthenticatorsPage = loadAsync(() => import('./AuthenticatorsPage'));
const AuthenticatorsEditPage = loadAsync(() => import('./AuthenticatorsEditPage'));
const ConfigurationsPage = loadAsync(() => import('./ConfigurationsPage'));
const ContentPacksPage = loadAsync(() => import('./ContentPacksPage'));
const CreateEventDefinitionPage = loadAsync(() => import('./CreateEventDefinitionPage'));
const CreateEventNotificationPage = loadAsync(() => import('./CreateEventNotificationPage'));
const CreateContentPackPage = loadAsync(() => import('pages/CreateContentPackPage'));
const CreateExtractorsPage = loadAsync(() => import('./CreateExtractorsPage'));
const DelegatedSearchPage = loadAsync(() => import('./DelegatedSearchPage'));
const EditEventDefinitionPage = loadAsync(() => import('./EditEventDefinitionPage'));
const EditEventNotificationPage = loadAsync(() => import('./EditEventNotificationPage'));
const EditContentPackPage = loadAsync(() => import('pages/EditContentPackPage'));
const EditExtractorsPage = loadAsync(() => import('./EditExtractorsPage'));
const EnterprisePage = loadAsync(() => import('./EnterprisePage'));
const EventDefinitionsPage = loadAsync(() => import('./EventDefinitionsPage'));
const EventNotificationsPage = loadAsync(() => import('./EventNotificationsPage'));
const EventsPage = loadAsync(() => import('./EventsPage'));
const ExportExtractorsPage = loadAsync(() => import('pages/ExportExtractorsPage'));
const ExtractorsPage = loadAsync(() => import('./ExtractorsPage'));
const WelcomePage = loadAsync(() => import('./WelcomePage'));
const GrokPatternsPage = loadAsync(() => import('./GrokPatternsPage'));
const ImportExtractorsPage = loadAsync(() => import('./ImportExtractorsPage'));
const IndexerFailuresPage = loadAsync(() => import('./IndexerFailuresPage'));
const IndexSetConfigurationPage = loadAsync(() => import('./IndexSetConfigurationPage'));
const IndexSetCreationPage = loadAsync(() => import('./IndexSetCreationPage'));
const IndexSetPage = loadAsync(() => import('./IndexSetPage'));
const IndicesPage = loadAsync(() => import('./IndicesPage'));
const InputsPage = loadAsync(() => import('./InputsPage'));
const LoadingPage = loadAsync(() => import(/* webpackChunkName: "LoadingPage" */ 'pages/LoadingPage'));
const LoggersPage = loadAsync(() => import('./LoggersPage'));
const LoginPage = loadAsync(() => import(/* webpackChunkName: "LoginPage" */ 'pages/LoginPage'));
const LUTCachesPage = loadAsync(() => import('./LUTCachesPage'));
const LUTDataAdaptersPage = loadAsync(() => import('./LUTDataAdaptersPage'));
const LUTTablesPage = loadAsync(() => import('./LUTTablesPage'));
const NodeInputsPage = loadAsync(() => import('./NodeInputsPage'));
const NodesPage = loadAsync(() => import('./NodesPage'));
const NotFoundPage = loadAsync(() => import('./NotFoundPage'));
const PipelineDetailsPage = loadAsync(() => import('./PipelineDetailsPage'));
const PipelinesOverviewPage = loadAsync(() => import('./PipelinesOverviewPage'));
const RoleDetailsPage = loadAsync(() => import('./RoleDetailsPage'));
const RoleEditPage = loadAsync(() => import('./RoleEditPage'));
const RolesOverviewPage = loadAsync(() => import('./RolesOverviewPage'));
const RuleDetailsPage = loadAsync(() => import('./RuleDetailsPage'));
const RulesPage = loadAsync(() => import('./RulesPage'));
const SecurityPage = loadAsync(() => import('./SecurityPage'));
const ShowContentPackPage = loadAsync(() => import('pages/ShowContentPackPage'));
const ShowEventNotificationPage = loadAsync(() => import('./ShowEventNotificationPage'));
const ShowMessagePage = loadAsync(() => import('./ShowMessagePage'));
const ShowMetricsPage = loadAsync(() => import('./ShowMetricsPage'));
const ShowNodePage = loadAsync(() => import('./ShowNodePage'));
const SidecarAdministrationPage = loadAsync(() => import('pages/SidecarAdministrationPage'));
const SidecarConfigurationPage = loadAsync(() => import('pages/SidecarConfigurationPage'));
const SidecarEditCollectorPage = loadAsync(() => import('pages/SidecarEditCollectorPage'));
const SidecarEditConfigurationPage = loadAsync(() => import('pages/SidecarEditConfigurationPage'));
const SidecarNewCollectorPage = loadAsync(() => import('pages/SidecarNewCollectorPage'));
const SidecarNewConfigurationPage = loadAsync(() => import('pages/SidecarNewConfigurationPage'));
const SidecarFailureTrackingPage = loadAsync(() => import('pages/SidecarFailureTrackingPage'));
const SidecarsPage = loadAsync(() => import('pages/SidecarsPage'));
const SidecarStatusPage = loadAsync(() => import('pages/SidecarStatusPage'));
const SimulatorPage = loadAsync(() => import('./SimulatorPage'));
const StartPage = loadAsync(() => import('./StartPage'));
const StreamEditPage = loadAsync(() => import('./StreamEditPage'));
const StreamOutputsPage = loadAsync(() => import('./StreamOutputsPage'));
const StreamsPage = loadAsync(() => import('./StreamsPage'));
const SystemOutputsPage = loadAsync(() => import('./SystemOutputsPage'));
const SystemOverviewPage = loadAsync(() => import('./SystemOverviewPage'));
const ThreadDumpPage = loadAsync(() => import('./ThreadDumpPage'));
const ProcessBufferDumpPage = loadAsync(() => import('./ProcessBufferDumpPage'));
const UserCreatePage = loadAsync(() => import('./UserCreatePage'));
const UserDetailsPage = loadAsync(() => import('./UserDetailsPage'));
const UserEditPage = loadAsync(() => import('./UserEditPage'));
const UserTokensEditPage = loadAsync(() => import('./UserTokensEditPage'));
const UsersOverviewPage = loadAsync(() => import('./UsersOverviewPage'));
const ViewEventDefinitionPage = loadAsync(() => import('./ViewEventDefinitionPage'));

export {
  AuthenticationCreatePage,
  AuthenticationPage,
  AuthenticationBackendCreatePage,
  AuthenticationBackendDetailsPage,
  AuthenticationBackendEditPage,
  AuthenticationOverviewPage,
  AuthenticatorsPage,
  AuthenticatorsEditPage,
  ConfigurationsPage,
  ContentPacksPage,
  CreateEventDefinitionPage,
  CreateEventNotificationPage,
  CreateContentPackPage,
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
  LoadingPage,
  LoggersPage,
  LoginPage,
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
  SidecarFailureTrackingPage,
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
  UsersOverviewPage,
  UserCreatePage,
  UserDetailsPage,
  UserEditPage,
  UserTokensEditPage,
  ViewEventDefinitionPage,
};
