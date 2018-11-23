import React from 'react';
import { IndexRoute, Redirect, Router, Route } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

import App from 'routing/App';
import AppWithSearchBar from 'routing/AppWithSearchBar';
import AppWithoutSearchBar from 'routing/AppWithoutSearchBar';
import AppWithGlobalNotifications from 'routing/AppWithGlobalNotifications';
import history from 'util/History';
import URLUtils from 'util/URLUtils';

import Routes from 'routing/Routes';

import {
  StartPage,
  DelegatedSearchPage,
  ShowMessagePage,
  StreamsPage,
  AlertsPage,
  ShowAlertPage,
  AlertConditionsPage,
  AlertNotificationsPage,
  NewAlertConditionPage,
  NewAlertNotificationPage,
  EditAlertConditionPage,
  StreamEditPage,
  StreamOutputsPage,
  StreamSearchPage,
  DashboardsPage,
  ShowDashboardPage,
  SourcesPage,
  InputsPage,
  NodeInputsPage,
  ExtractorsPage,
  CreateExtractorsPage,
  EditExtractorsPage,
  ImportExtractorsPage,
  ExportExtractorsPage,
  SystemOutputsPage,
  RolesPage,
  ContentPacksPage,
  ShowContentPackPage,
  CreateContentPackPage,
  EditContentPackPage,
  UsersPage,
  CreateUsersPage,
  EditUsersPage,
  EditTokensPage,
  GrokPatternsPage,
  SystemOverviewPage,
  IndexerFailuresPage,
  IndicesPage,
  LoggersPage,
  GettingStartedPage,
  ShowMetricsPage,
  ShowNodePage,
  NodesPage,
  ThreadDumpPage,
  ConfigurationsPage,
  NotFoundPage,
  AuthenticationPage,
  IndexSetPage,
  IndexSetConfigurationPage,
  IndexSetCreationPage,
  LUTTablesPage,
  LUTCachesPage,
  LUTDataAdaptersPage,
  PipelinesOverviewPage,
  PipelineDetailsPage,
  SimulatorPage,
  RulesPage,
  RuleDetailsPage,
  EnterprisePage,
  SidecarEditConfigurationPage,
  SidecarStatusPage,
  SidecarAdministrationPage,
  SidecarEditCollectorPage,
  SidecarNewCollectorPage,
  SidecarsPage,
  SidecarConfigurationPage,
  SidecarNewConfigurationPage,
  StreamAlertsOverviewPage,
} from 'pages';

const AppRouter = () => {
  const pluginRoutes = PluginStore.exports('routes').map((pluginRoute) => {
    return (<Route key={`${pluginRoute.path}-${pluginRoute.component.displayName}`}
                  path={URLUtils.appPrefixed(pluginRoute.path)}
                  component={pluginRoute.component} />);
  });
  return (
    <Router history={history}>
      <Route path={Routes.STARTPAGE} component={App}>
        <Route component={AppWithGlobalNotifications}>
          <IndexRoute component={StartPage} />
          <Route component={AppWithSearchBar}>
            <Route path={Routes.SEARCH} component={DelegatedSearchPage} />
            <Route path={Routes.message_show(':index', ':messageId')} component={ShowMessagePage} />
            <Route path={Routes.SOURCES} component={SourcesPage} />
            <Route path={Routes.stream_search(':streamId')} component={StreamSearchPage} />
            <Redirect from={Routes.legacy_stream_search(':streamId')} to={Routes.stream_search(':streamId')} />
          </Route>
          <Route component={AppWithoutSearchBar}>
            <Route path={Routes.GETTING_STARTED} component={GettingStartedPage} />
            <Route path={Routes.STREAMS} component={StreamsPage} />
            <Route path={Routes.stream_edit(':streamId')} component={StreamEditPage} />
            <Route path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage} />
            <Route path={Routes.stream_alerts(':streamId')} component={StreamAlertsOverviewPage} />
            <Route path={Routes.ALERTS.LIST} component={AlertsPage} />
            <Route path={Routes.ALERTS.CONDITIONS} component={AlertConditionsPage} />
            <Route path={Routes.ALERTS.NEW_CONDITION} component={NewAlertConditionPage} />
            <Route path={Routes.ALERTS.NOTIFICATIONS} component={AlertNotificationsPage} />
            <Route path={Routes.ALERTS.NEW_NOTIFICATION} component={NewAlertNotificationPage} />
            <Route path={Routes.show_alert_condition(':streamId', ':conditionId')} component={EditAlertConditionPage} />
            <Route path={Routes.show_alert(':alertId')} component={ShowAlertPage} />
            <Route path={Routes.DASHBOARDS} component={DashboardsPage} />
            <Route path={Routes.dashboard_show(':dashboardId')} component={ShowDashboardPage} />
            <Route path={Routes.SYSTEM.INPUTS} component={InputsPage} />
            <Route path={Routes.node_inputs(':nodeId')} component={NodeInputsPage} />
            <Route path={Routes.global_input_extractors(':inputId')} component={ExtractorsPage} />
            <Route path={Routes.local_input_extractors(':nodeId', ':inputId')} component={ExtractorsPage} />
            <Route path={Routes.new_extractor(':nodeId', ':inputId')} component={CreateExtractorsPage} />
            <Route path={Routes.edit_extractor(':nodeId', ':inputId', ':extractorId')} component={EditExtractorsPage} />
            <Route path={Routes.import_extractors(':nodeId', ':inputId')} component={ImportExtractorsPage} />
            <Route path={Routes.export_extractors(':nodeId', ':inputId')} component={ExportExtractorsPage} />
            <Route path={Routes.SYSTEM.CONFIGURATIONS} component={ConfigurationsPage} />
            <Route path={Routes.SYSTEM.CONTENTPACKS.LIST} component={ContentPacksPage} />
            <Route path={Routes.SYSTEM.CONTENTPACKS.CREATE} component={CreateContentPackPage} />
            <Route path={Routes.SYSTEM.CONTENTPACKS.edit(':contentPackId', ':contentPackRev')} component={EditContentPackPage} />
            <Route path={Routes.SYSTEM.CONTENTPACKS.show(':contentPackId')} component={ShowContentPackPage} />
            <Route path={Routes.SYSTEM.GROKPATTERNS} component={GrokPatternsPage} />
            <Route path={Routes.SYSTEM.INDICES.LIST} component={IndicesPage} />
            <Route path={Routes.SYSTEM.INDEX_SETS.CREATE} component={IndexSetCreationPage} />
            <Route path={Routes.SYSTEM.INDEX_SETS.SHOW(':indexSetId')} component={IndexSetPage} />
            <Route path={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(':indexSetId')} component={IndexSetConfigurationPage} />
            <Route path={Routes.SYSTEM.INDICES.FAILURES} component={IndexerFailuresPage} />

            <Route path={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} component={LUTTablesPage} />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.CREATE} component={LUTTablesPage} action="create" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.show(':tableName')} component={LUTTablesPage} action="show" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.edit(':tableName')} component={LUTTablesPage} action="edit" />

            <Route path={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} component={LUTCachesPage} />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE} component={LUTCachesPage} action="create" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(':cacheName')} component={LUTCachesPage} action="show" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(':cacheName')} component={LUTCachesPage} action="edit" />

            <Route path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW} component={LUTDataAdaptersPage} />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE} component={LUTDataAdaptersPage} action="create" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(':adapterName')} component={LUTDataAdaptersPage} action="show" />
            <Route path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(':adapterName')} component={LUTDataAdaptersPage} action="edit" />

            <Route path={Routes.SYSTEM.PIPELINES.OVERVIEW} component={PipelinesOverviewPage} />
            <Route path={Routes.SYSTEM.PIPELINES.RULES} component={RulesPage} />
            <Route path={Routes.SYSTEM.PIPELINES.RULE(':ruleId')} component={RuleDetailsPage} />
            <Route path={Routes.SYSTEM.PIPELINES.SIMULATOR} component={SimulatorPage} />
            <Route path={Routes.SYSTEM.PIPELINES.PIPELINE(':pipelineId')} component={PipelineDetailsPage} />

            <Route path={Routes.SYSTEM.LOGGING} component={LoggersPage} />
            <Route path={Routes.SYSTEM.METRICS(':nodeId')} component={ShowMetricsPage} />
            <Route path={Routes.SYSTEM.NODES.LIST} component={NodesPage} />
            <Route path={Routes.SYSTEM.NODES.SHOW(':nodeId')} component={ShowNodePage} />
            <Route path={Routes.SYSTEM.OUTPUTS} component={SystemOutputsPage} />
            <Route path={Routes.SYSTEM.AUTHENTICATION.OVERVIEW} component={AuthenticationPage}>
              <IndexRoute component={UsersPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.USERS.LIST} component={UsersPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.USERS.CREATE} component={CreateUsersPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.USERS.edit(':username')} component={EditUsersPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.USERS.TOKENS.edit(':username')} component={EditTokensPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.ROLES} component={RolesPage} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CONFIG} />
              <Route path={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.provider(':name')} />
            </Route>
            <Route path={Routes.SYSTEM.OVERVIEW} component={SystemOverviewPage} />
            <Route path={Routes.SYSTEM.THREADDUMP(':nodeId')} component={ThreadDumpPage} />
            <Route path={Routes.SYSTEM.ENTERPRISE} component={EnterprisePage} />

            <Route path={Routes.SYSTEM.SIDECARS.OVERVIEW} component={SidecarsPage} />
            <Route path={Routes.SYSTEM.SIDECARS.STATUS(':sidecarId')} component={SidecarStatusPage} />
            <Route path={Routes.SYSTEM.SIDECARS.ADMINISTRATION} component={SidecarAdministrationPage} />
            <Route path={Routes.SYSTEM.SIDECARS.CONFIGURATION} component={SidecarConfigurationPage} />
            <Route path={Routes.SYSTEM.SIDECARS.NEW_CONFIGURATION} component={SidecarNewConfigurationPage} />
            <Route path={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(':configurationId')} component={SidecarEditConfigurationPage} />
            <Route path={Routes.SYSTEM.SIDECARS.NEW_COLLECTOR} component={SidecarNewCollectorPage} />
            <Route path={Routes.SYSTEM.SIDECARS.EDIT_COLLECTOR(':collectorId')} component={SidecarEditCollectorPage} />
            {pluginRoutes}
          </Route>
        </Route>
        <Route component={AppWithoutSearchBar}>
          <Route path={Routes.NOTFOUND} component={NotFoundPage} />
          <Route path="*" component={NotFoundPage} />
        </Route>
      </Route>
    </Router>
  );
};

export default AppRouter;
