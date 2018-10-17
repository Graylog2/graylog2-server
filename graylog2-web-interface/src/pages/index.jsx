import loadAsync from 'routing/loadAsync';

const AlertConditionsPage = loadAsync(() => import('./AlertConditionsPage'));
const AlertNotificationsPage = loadAsync(() => import('./AlertNotificationsPage'));
const AlertsPage = loadAsync(() => import('./AlertsPage'));
const AuthenticationPage = loadAsync(() => import('./AuthenticationPage'));
const ConfigurationsPage = loadAsync(() => import('./ConfigurationsPage'));
const ContentPacksPage = loadAsync(() => import('./ContentPacksPage'));
const CreateExtractorsPage = loadAsync(() => import('./CreateExtractorsPage'));
const CreateUsersPage = loadAsync(() => import('./CreateUsersPage'));
const DashboardsPage = loadAsync(() => import('./DashboardsPage'));
const DelegatedSearchPage = loadAsync(() => import('./DelegatedSearchPage'));
const EditAlertConditionPage = loadAsync(() => import('./EditAlertConditionPage'));
const EditExtractorsPage = loadAsync(() => import('./EditExtractorsPage'));
const EditTokensPage = loadAsync(() => import('./EditTokensPage'));
const EditUsersPage = loadAsync(() => import('./EditUsersPage'));
const EnterprisePage = loadAsync(() => import('./EnterprisePage'));
const ShowContentPackPage = loadAsync(() => import('pages/ShowContentPackPage'));
const CreateContentPackPage = loadAsync(() => import('pages/CreateContentPackPage'));
const EditContentPackPage = loadAsync(() => import('pages/EditContentPackPage'));
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
const LoggedInPage = loadAsync(() => import(/* webpackChunkName: "LoggedInPage" */ 'pages/LoggedInPage'));
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
const ShowDashboardPage = loadAsync(() => import('./ShowDashboardPage'));
const ShowMessagePage = loadAsync(() => import('./ShowMessagePage'));
const ShowMetricsPage = loadAsync(() => import('./ShowMetricsPage'));
const ShowNodePage = loadAsync(() => import('./ShowNodePage'));
const SimulatorPage = loadAsync(() => import('./SimulatorPage'));
const SourcesPage = loadAsync(() => import('./SourcesPage'));
const StartPage = loadAsync(() => import('./StartPage'));
const StreamEditPage = loadAsync(() => import('./StreamEditPage'));
const StreamOutputsPage = loadAsync(() => import('./StreamOutputsPage'));
const StreamSearchPage = loadAsync(() => import('./StreamSearchPage'));
const StreamsPage = loadAsync(() => import('./StreamsPage'));
const SystemOutputsPage = loadAsync(() => import('./SystemOutputsPage'));
const SystemOverviewPage = loadAsync(() => import('./SystemOverviewPage'));
const ThreadDumpPage = loadAsync(() => import('./ThreadDumpPage'));
const UsersPage = loadAsync(() => import('./UsersPage'));
const SidecarEditConfigurationPage = loadAsync(() => import('pages/SidecarEditConfigurationPage'));
const SidecarStatusPage = loadAsync(() => import('pages/SidecarStatusPage'));
const SidecarAdministrationPage = loadAsync(() => import('pages/SidecarAdministrationPage'));
const SidecarEditCollectorPage = loadAsync(() => import('pages/SidecarEditCollectorPage'));
const SidecarNewCollectorPage = loadAsync(() => import('pages/SidecarNewCollectorPage'));
const SidecarsPage = loadAsync(() => import('pages/SidecarsPage'));
const SidecarConfigurationPage = loadAsync(() => import('pages/SidecarConfigurationPage'));
const SidecarNewConfigurationPage = loadAsync(() => import('pages/SidecarNewConfigurationPage'));
const StreamAlertsOverviewPage = loadAsync(() => import('pages/StreamAlertsOverviewPage'));

export {
  AlertConditionsPage,
  AlertNotificationsPage,
  AlertsPage,
  AuthenticationPage,
  ConfigurationsPage,
  ContentPacksPage,
  CreateExtractorsPage,
  CreateUsersPage,
  DashboardsPage,
  DelegatedSearchPage,
  EditAlertConditionPage,
  EditExtractorsPage,
  EditTokensPage,
  EditUsersPage,
  EnterprisePage,
  ExportExtractorsPage,
  ShowContentPackPage,
  CreateContentPackPage,
  EditContentPackPage,
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
  LoggedInPage,
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
  RolesPage,
  RuleDetailsPage,
  RulesPage,
  ShowAlertPage,
  ShowDashboardPage,
  ShowMessagePage,
  ShowMetricsPage,
  ShowNodePage,
  SimulatorPage,
  SourcesPage,
  StartPage,
  StreamEditPage,
  StreamOutputsPage,
  StreamSearchPage,
  StreamsPage,
  SystemOutputsPage,
  SystemOverviewPage,
  ThreadDumpPage,
  UsersPage,
  SidecarEditConfigurationPage,
  SidecarStatusPage,
  SidecarAdministrationPage,
  SidecarEditCollectorPage,
  SidecarNewCollectorPage,
  SidecarsPage,
  SidecarConfigurationPage,
  SidecarNewConfigurationPage,
  StreamAlertsOverviewPage,
};
