import React from 'react';
import { Redirect, Router, Route, Switch } from 'react-router-dom';
import { PluginStore } from 'graylog-web-plugin/plugin';

import App from 'routing/App';
import AppWithoutSearchBar from 'routing/AppWithoutSearchBar';
import AppWithGlobalNotifications from 'routing/AppWithGlobalNotifications';
import history from 'util/History';
import Routes from 'routing/Routes';
import { appPrefixed } from 'util/URLUtils';
import {
  AlertConditionsPage,
  AlertNotificationsPage,
  AlertsPage,
  AuthenticationCreatePage,
  AuthenticationBackendCreatePage,
  AuthenticationBackendDetailsPage,
  AuthenticationBackendEditPage,
  AuthenticationOverviewPage,
  AuthenticationPage,
  ConfigurationsPage,
  ContentPacksPage,
  CreateContentPackPage,
  CreateEventDefinitionPage,
  CreateEventNotificationPage,
  CreateExtractorsPage,
  DelegatedSearchPage,
  EditAlertConditionPage,
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
  GettingStartedPage,
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
  NewAlertConditionPage,
  NewAlertNotificationPage,
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
  UserDetailsPage,
  UserCreatePage,
  UserEditPage,
  UserTokensEditPage,
  UsersOverviewPage,
} from 'pages';
import RouterErrorBoundary from 'components/errors/RouterErrorBoundary';

import {} from 'components/authentication';
import AuthProvidersConfig from '../components/authentication/AuthProvidersConfig';

const renderPluginRoute = ({ path, component: Component }) => (
  <Route key={`${path}-${Component.displayName}`}
         path={URLUtils.appPrefixed(path)}>
    <Component />
  </Route>
);

const AppRouter = () => {
  const pluginRoutes = PluginStore.exports('routes');
  const pluginRoutesWithNullParent = pluginRoutes.filter((route) => (route.parentComponent === null)).map(renderPluginRoute);
  const pluginRoutesWithParent = pluginRoutes.filter((route) => route.parentComponent).map((pluginRoute) => {
    const PluginRouteComponent = pluginRoute.component;
    const PluginParentComponent = pluginRoute.parentComponent;

    return (
      <Route key={`${pluginRoute.path}-${pluginRoute.component.displayName}`} exact path={URLUtils.appPrefixed(pluginRoute.path)}>
        <PluginParentComponent>
          <PluginRouteComponent />
        </PluginParentComponent>
      </Route>
    );
  });
  const standardPluginRoutes = pluginRoutes.filter((route) => (route.parentComponent === undefined)).map(renderPluginRoute);

  return (
    <Router history={history}>
      <Switch>
        <RouterErrorBoundary>
          {pluginRoutesWithNullParent}

          <App>
            <AppWithGlobalNotifications>
              <Route path={Routes.STARTPAGE}>
                <Switch>
                  <Route exact path={Routes.STARTPAGE} component={StartPage} />
                  {pluginRoutesWithParent}
                  <Route exact path={Routes.SEARCH} component={DelegatedSearchPage} />
                  <Route path="/">
                    <AppWithoutSearchBar>
                      <Switch>
                        <Route path={Routes.message_show(':index', ':messageId')} component={ShowMessagePage} />
                        <Redirect from={Routes.legacy_stream_search(':streamId')} to={Routes.stream_search(':streamId')} />
                        <Route path={Routes.GETTING_STARTED} component={GettingStartedPage} />
                        <Route path={Routes.STREAMS} component={StreamsPage} />
                        <Route path={Routes.stream_edit(':streamId')} component={StreamEditPage} />
                        <Route path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage} />
                        <Route path={Routes.stream_alerts(':streamId')} component={StreamAlertsOverviewPage} />
                        <Route path={Routes.LEGACY_ALERTS.LIST} component={AlertsPage} />
                        <Route path={Routes.LEGACY_ALERTS.CONDITIONS} component={AlertConditionsPage} />
                        <Route path={Routes.LEGACY_ALERTS.NEW_CONDITION} component={NewAlertConditionPage} />
                        <Route path={Routes.LEGACY_ALERTS.NOTIFICATIONS} component={AlertNotificationsPage} />
                        <Route path={Routes.LEGACY_ALERTS.NEW_NOTIFICATION} component={NewAlertNotificationPage} />
                        <Route exact path={Routes.ALERTS.LIST} component={EventsPage} />
                        <Route exact path={Routes.ALERTS.DEFINITIONS.LIST} component={EventDefinitionsPage} />
                        <Route exact path={Routes.ALERTS.DEFINITIONS.CREATE} component={CreateEventDefinitionPage} />
                        <Route exact
                               path={Routes.ALERTS.DEFINITIONS.edit(':definitionId')}
                               component={EditEventDefinitionPage} />
                        <Route exact path={Routes.ALERTS.NOTIFICATIONS.LIST} component={EventNotificationsPage} />
                        <Route exact path={Routes.ALERTS.NOTIFICATIONS.CREATE} component={CreateEventNotificationPage} />
                        <Route exact
                               path={Routes.ALERTS.NOTIFICATIONS.edit(':notificationId')}
                               component={EditEventNotificationPage} />
                        <Route path={Routes.show_alert_condition(':streamId', ':conditionId')}
                               component={EditAlertConditionPage} />
                        <Route path={Routes.show_alert(':alertId')} component={ShowAlertPage} />
                        <Route path={Routes.SYSTEM.INPUTS} component={InputsPage} />
                        <Route path={Routes.node_inputs(':nodeId')} component={NodeInputsPage} />
                        <Route path={Routes.global_input_extractors(':inputId')} component={ExtractorsPage} />
                        <Route path={Routes.local_input_extractors(':nodeId', ':inputId')} component={ExtractorsPage} />
                        <Route path={Routes.new_extractor(':nodeId', ':inputId')} component={CreateExtractorsPage} />
                        <Route path={Routes.edit_extractor(':nodeId', ':inputId', ':extractorId')}
                               component={EditExtractorsPage} />
                        <Route path={Routes.import_extractors(':nodeId', ':inputId')} component={ImportExtractorsPage} />
                        <Route path={Routes.export_extractors(':nodeId', ':inputId')} component={ExportExtractorsPage} />
                        <Route path={Routes.SYSTEM.CONFIGURATIONS} component={ConfigurationsPage} />
                        <Route path={Routes.SYSTEM.CONTENTPACKS.LIST} component={ContentPacksPage} />
                        <Route path={Routes.SYSTEM.CONTENTPACKS.CREATE} component={CreateContentPackPage} />
                        <Route path={Routes.SYSTEM.CONTENTPACKS.edit(':contentPackId', ':contentPackRev')}
                               component={EditContentPackPage} />
                        <Route path={Routes.SYSTEM.CONTENTPACKS.show(':contentPackId')}
                               component={ShowContentPackPage} />
                        <Route path={Routes.SYSTEM.GROKPATTERNS} component={GrokPatternsPage} />
                        <Route path={Routes.SYSTEM.INDICES.LIST} component={IndicesPage} />
                        <Route path={Routes.SYSTEM.INDEX_SETS.CREATE} component={IndexSetCreationPage} />
                        <Route path={Routes.SYSTEM.INDEX_SETS.SHOW(':indexSetId')} component={IndexSetPage} />
                        <Route path={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(':indexSetId')}
                               component={IndexSetConfigurationPage} />
                        <Route path={Routes.SYSTEM.INDICES.FAILURES} component={IndexerFailuresPage} />

                        <Route exact path={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} component={LUTTablesPage} />
                        <Route exact path={Routes.SYSTEM.LOOKUPTABLES.CREATE}>
                          <LUTTablesPage action="create" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.show(':tableName')}>
                          <LUTTablesPage action="show" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.edit(':tableName')}>
                          <LUTTablesPage action="edit" />
                        </Route>

                        <Route exact path={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} component={LUTCachesPage} />
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}>
                          <LUTCachesPage action="create" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(':cacheName')}>
                          <LUTCachesPage action="show" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(':cacheName')}>
                          <LUTCachesPage action="edit" />
                        </Route>

                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}
                               component={LUTDataAdaptersPage} />
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}>
                          <LUTDataAdaptersPage action="create" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(':adapterName')}>
                          <LUTDataAdaptersPage action="show" />
                        </Route>
                        <Route exact
                               path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(':adapterName')}>
                          <LUTDataAdaptersPage action="edit" />
                        </Route>

                        <Route exact path={Routes.SYSTEM.PIPELINES.OVERVIEW} component={PipelinesOverviewPage} />
                        <Route exact path={Routes.SYSTEM.PIPELINES.RULES} component={RulesPage} />
                        <Route exact path={Routes.SYSTEM.PIPELINES.RULE(':ruleId')} component={RuleDetailsPage} />
                        <Route exact path={Routes.SYSTEM.PIPELINES.SIMULATOR} component={SimulatorPage} />
                        <Route exact path={Routes.SYSTEM.PIPELINES.PIPELINE(':pipelineId')} component={PipelineDetailsPage} />

                        <Route path={Routes.SYSTEM.LOGGING} component={LoggersPage} />
                        <Route path={Routes.SYSTEM.METRICS(':nodeId')} component={ShowMetricsPage} />
                        <Route exact path={Routes.SYSTEM.NODES.LIST} component={NodesPage} />
                        <Route exact path={Routes.SYSTEM.NODES.SHOW(':nodeId')} component={ShowNodePage} />
                        <Route path={Routes.SYSTEM.OUTPUTS} component={SystemOutputsPage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE} component={AuthenticationPage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE} component={AuthenticationCreatePage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW} component={AuthenticationOverviewPage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(':backendId')} component={AuthenticationBackendDetailsPage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(':backendId')} component={AuthenticationBackendEditPage} />
                        <Route path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(':name')} component={AuthenticationBackendCreatePage} />

                        <Route path={Routes.SYSTEM.USERS.OVERVIEW} component={UsersOverviewPage} />
                        <Route path={Routes.SYSTEM.USERS.CREATE} component={UserCreatePage} />
                        <Route path={Routes.SYSTEM.USERS.show(':username')} component={UserDetailsPage} />
                        <Route path={Routes.SYSTEM.USERS.edit(':username')} component={UserEditPage} />
                        <Route path={Routes.SYSTEM.USERS.TOKENS.edit(':username')} component={UserTokensEditPage} />

                        <Route path={Routes.SYSTEM.AUTHZROLES.OVERVIEW} component={RolesOverviewPage} />
                        <Route path={Routes.SYSTEM.AUTHZROLES.show(':roleId')} component={RoleDetailsPage} />
                        <Route path={Routes.SYSTEM.AUTHZROLES.edit(':roleId')} component={RoleEditPage} />

                        <Route path={Routes.SYSTEM.OVERVIEW} component={SystemOverviewPage} />
                        <Route path={Routes.SYSTEM.PROCESSBUFFERDUMP(':nodeId')} component={ProcessBufferDumpPage} />
                        <Route path={Routes.SYSTEM.THREADDUMP(':nodeId')} component={ThreadDumpPage} />
                        <Route path={Routes.SYSTEM.ENTERPRISE} component={EnterprisePage} />

                        <Route exact path={Routes.SYSTEM.SIDECARS.OVERVIEW} component={SidecarsPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.STATUS(':sidecarId')} component={SidecarStatusPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.ADMINISTRATION} component={SidecarAdministrationPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.CONFIGURATION} component={SidecarConfigurationPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.NEW_CONFIGURATION} component={SidecarNewConfigurationPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(':configurationId')}
                               component={SidecarEditConfigurationPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.NEW_COLLECTOR} component={SidecarNewCollectorPage} />
                        <Route exact path={Routes.SYSTEM.SIDECARS.EDIT_COLLECTOR(':collectorId')}
                               component={SidecarEditCollectorPage} />
                        {standardPluginRoutes}
                      </Switch>
                    </AppWithoutSearchBar>
                  </Route>
                  <Route exact path={Routes.NOTFOUND}>
                    <AppWithoutSearchBar>
                      <NotFoundPage />
                    </AppWithoutSearchBar>
                  </Route>
                  <Route path="*">
                    <AppWithoutSearchBar>
                      <NotFoundPage />
                    </AppWithoutSearchBar>
                  </Route>
                </Switch>
              </Route>
            </AppWithGlobalNotifications>
            <Route exact path={Routes.NOTFOUND}>
              <AppWithoutSearchBar>
                <NotFoundPage />
              </AppWithoutSearchBar>
            </Route>
          </App>
        </RouterErrorBoundary>
      </Switch>
    </Router>
  );
};

export default AppRouter;
