import loadAsync from 'routing/loadAsync';

const AlertConditionsPage = loadAsync(() => import('./AlertConditionsPage'));
const AlertNotificationsPage = loadAsync(() => import('./AlertNotificationsPage'));
const AlertsPage = loadAsync(() => import('./AlertsPage'));
const AuthenticationPage = loadAsync(() => import('./AuthenticationPage'));
const ConfigurationsPage = loadAsync(() => import('./ConfigurationsPage'));
const ContentPacksPage = loadAsync(() => import('./ContentPacksPage'));
const CreateEventDefinitionPage = loadAsync(() => import('./CreateEventDefinitionPage'));
const CreateEventNotificationPage = loadAsync(() => import('./CreateEventNotificationPage'));
const CreateContentPackPage = loadAsync(() => import('pages/CreateContentPackPage'));
const CreateExtractorsPage = loadAsync(() => import('./CreateExtractorsPage'));
const CreateUsersPage = loadAsync(() => import('./CreateUsersPage'));
const DelegatedSearchPage = loadAsync(() => import('./DelegatedSearchPage'));
const EditAlertConditionPage = loadAsync(() => import('./EditAlertConditionPage'));
const EditEventDefinitionPage = loadAsync(() => import('./EditEventDefinitionPage'));
const EditEventNotificationPage = loadAsync(() => import('./EditEventNotificationPage'));
const EditContentPackPage = loadAsync(() => import('pages/EditContentPackPage'));
const EditExtractorsPage = loadAsync(() => import('./EditExtractorsPage'));
const EditTokensPage = loadAsync(() => import('./EditTokensPage'));
const EditUsersPage = loadAsync(() => import('./EditUsersPage'));
const EnterprisePage = loadAsync(() => import('./EnterprisePage'));
const EventDefinitionsPage = loadAsync(() => import('./EventDefinitionsPage'));
const EventNotificationsPage = loadAsync(() => import('./EventNotificationsPage'));
const EventsPage = loadAsync(() => import('./EventsPage'));
const ExportExtractorsPage = loadAsync(() => import('pages/ExportExtractorsPage'));
const ExtractorsPage = loadAsync(() => import('./ExtractorsPage'));
const GettingStartedPage = loadAsync(() => import('./GettingStartedPage'));
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
const NewAlertConditionPage = loadAsync(() => import('./NewAlertConditionPage'));
const NewAlertNotificationPage = loadAsync(() => import('./NewAlertNotificationPage'));
const NodeInputsPage = loadAsync(() => import('./NodeInputsPage'));
const NodesPage = loadAsync(() => import('./NodesPage'));
const NotFoundPage = loadAsync(() => import('./NotFoundPage'));
const PipelineDetailsPage = loadAsync(() => import('./PipelineDetailsPage'));
const PipelinesOverviewPage = loadAsync(() => import('./PipelinesOverviewPage'));
const RolesPage = loadAsync(() => import('./RolesPage'));
const RuleDetailsPage = loadAsync(() => import('./RuleDetailsPage'));
const RulesPage = loadAsync(() => import('./RulesPage'));
const ShowAlertPage = loadAsync(() => import('./ShowAlertPage'));
const ShowContentPackPage = loadAsync(() => import('pages/ShowContentPackPage'));
const ShowMessagePage = loadAsync(() => import('./ShowMessagePage'));
const ShowMetricsPage = loadAsync(() => import('./ShowMetricsPage'));
const ShowNodePage = loadAsync(() => import('./ShowNodePage'));
const SidecarAdministrationPage = loadAsync(() => import('pages/SidecarAdministrationPage'));
const SidecarConfigurationPage = loadAsync(() => import('pages/SidecarConfigurationPage'));
const SidecarEditCollectorPage = loadAsync(() => import('pages/SidecarEditCollectorPage'));
const SidecarEditConfigurationPage = loadAsync(() => import('pages/SidecarEditConfigurationPage'));
const SidecarNewCollectorPage = loadAsync(() => import('pages/SidecarNewCollectorPage'));
const SidecarNewConfigurationPage = loadAsync(() => import('pages/SidecarNewConfigurationPage'));
const SidecarsPage = loadAsync(() => import('pages/SidecarsPage'));
const SidecarStatusPage = loadAsync(() => import('pages/SidecarStatusPage'));
const SimulatorPage = loadAsync(() => import('./SimulatorPage'));
const StartPage = loadAsync(() => import('./StartPage'));
const StreamAlertsOverviewPage = loadAsync(() => import('pages/StreamAlertsOverviewPage'));
const StreamEditPage = loadAsync(() => import('./StreamEditPage'));
const StreamOutputsPage = loadAsync(() => import('./StreamOutputsPage'));
const StreamsPage = loadAsync(() => import('./StreamsPage'));
const SystemOutputsPage = loadAsync(() => import('./SystemOutputsPage'));
const SystemOverviewPage = loadAsync(() => import('./SystemOverviewPage'));
const ThreadDumpPage = loadAsync(() => import('./ThreadDumpPage'));
const ProcessBufferDumpPage = loadAsync(() => import('./ProcessBufferDumpPage'));
const UsersPage = loadAsync(() => import('./UsersPage'));

export {
  AlertConditionsPage,
  AlertNotificationsPage,
  AlertsPage,
  AuthenticationPage,
  ConfigurationsPage,
  ContentPacksPage,
  CreateEventDefinitionPage,
  CreateEventNotificationPage,
  CreateContentPackPage,
  CreateExtractorsPage,
  CreateUsersPage,
  DelegatedSearchPage,
  EditAlertConditionPage,
  EditEventDefinitionPage,
  EditEventNotificationPage,
  EditContentPackPage,
  EditExtractorsPage,
  EditTokensPage,
  EditUsersPage,
  EnterprisePage,
  EventDefinitionsPage,
  EventNotificationsPage,
  EventsPage,
  ExportExtractorsPage,
  ExtractorsPage,
  GettingStartedPage,
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
  NewAlertConditionPage,
  NewAlertNotificationPage,
  NodeInputsPage,
  NodesPage,
  NotFoundPage,
  PipelineDetailsPage,
  PipelinesOverviewPage,
  ProcessBufferDumpPage,
  RolesPage,
  RuleDetailsPage,
  RulesPage,
  ShowAlertPage,
  ShowContentPackPage,
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
  StreamAlertsOverviewPage,
  StreamEditPage,
  StreamOutputsPage,
  StreamsPage,
  SystemOutputsPage,
  SystemOverviewPage,
  ThreadDumpPage,
  UsersPage,
};
