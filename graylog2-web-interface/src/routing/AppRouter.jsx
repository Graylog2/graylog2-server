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
import { Redirect, Router, Route, Switch } from 'react-router-dom';

import App from 'routing/App';
import PageContentLayout from 'components/layout/PageContentLayout';
import history from 'util/History';
import AppConfig from 'util/AppConfig';
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
  AuthenticatorsPage,
  AuthenticatorsEditPage,
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
} from 'pages';
import RouterErrorBoundary from 'components/errors/RouterErrorBoundary';
import usePluginEntities from 'views/logic/usePluginEntities';

const renderPluginRoute = ({ path, component: Component, parentComponent }) => {
  const ParentComponent = parentComponent ?? React.Fragment;
  const WrappedComponent = () => (
    <ParentComponent>
      <Component />
    </ParentComponent>
  );

  return (
    <Route key={`${path}-${Component.displayName}`}
           exact
           path={appPrefixed(path)}
           render={WrappedComponent} />
  );
};

const routeHasAppParent = (route) => route.parentComponent === App;

const AppRouter = () => {
  const pluginRoutes = usePluginEntities('routes');
  const pluginRoutesWithNullParent = pluginRoutes.filter((route) => (route.parentComponent === null)).map(renderPluginRoute);
  const pluginRoutesWithAppParent = pluginRoutes.filter((route) => routeHasAppParent(route)).map((route) => renderPluginRoute({ ...route, parentComponent: null }));
  const pluginRoutesWithParent = pluginRoutes.filter((route) => (route.parentComponent && !routeHasAppParent(route))).map(renderPluginRoute);
  const standardPluginRoutes = pluginRoutes.filter((route) => (route.parentComponent === undefined)).map(renderPluginRoute);

  const isCloud = AppConfig.isCloud();

  return (
    <Router history={history}>
      <RouterErrorBoundary>
        <Switch>
          {pluginRoutesWithNullParent}

          <Route path={Routes.STARTPAGE}>
            <App>
              <Switch>
                <Route exact path={Routes.STARTPAGE} component={StartPage} />
                <Route exact path={Routes.SEARCH} component={DelegatedSearchPage} />
                {pluginRoutesWithParent}
                {pluginRoutesWithAppParent}
                <Route path="/">
                  <PageContentLayout>
                    <Switch>
                      <Route exact path={Routes.message_show(':index', ':messageId')} component={ShowMessagePage} />
                      <Redirect from={Routes.legacy_stream_search(':streamId')} to={Routes.stream_search(':streamId')} />
                      <Route exact path={Routes.GETTING_STARTED} component={GettingStartedPage} />
                      <Route exact path={Routes.STREAMS} component={StreamsPage} />
                      <Route exact path={Routes.stream_edit(':streamId')} component={StreamEditPage} />
                      {!isCloud && <Route exact path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage} />}

                      <Route exact path={Routes.LEGACY_ALERTS.LIST} component={AlertsPage} />
                      <Route exact path={Routes.LEGACY_ALERTS.CONDITIONS} component={AlertConditionsPage} />
                      <Route exact path={Routes.LEGACY_ALERTS.NEW_CONDITION} component={NewAlertConditionPage} />
                      <Route exact path={Routes.LEGACY_ALERTS.NOTIFICATIONS} component={AlertNotificationsPage} />
                      <Route exact path={Routes.LEGACY_ALERTS.NEW_NOTIFICATION} component={NewAlertNotificationPage} />

                      <Route exact path={Routes.ALERTS.LIST} component={EventsPage} />
                      <Route exact path={Routes.ALERTS.DEFINITIONS.LIST} component={EventDefinitionsPage} />
                      <Route exact path={Routes.ALERTS.DEFINITIONS.CREATE} component={CreateEventDefinitionPage} />
                      <Route exact
                             path={Routes.ALERTS.DEFINITIONS.edit(':definitionId')}
                             component={EditEventDefinitionPage} />
                      <Route exact
                             path={Routes.ALERTS.DEFINITIONS.show(':definitionId')}
                             component={ViewEventDefinitionPage} />
                      <Route exact path={Routes.ALERTS.NOTIFICATIONS.LIST} component={EventNotificationsPage} />
                      <Route exact path={Routes.ALERTS.NOTIFICATIONS.CREATE} component={CreateEventNotificationPage} />
                      <Route exact
                             path={Routes.ALERTS.NOTIFICATIONS.edit(':notificationId')}
                             component={EditEventNotificationPage} />
                      <Route exact
                             path={Routes.ALERTS.NOTIFICATIONS.show(':notificationId')}
                             component={ShowEventNotificationPage} />
                      <Route exact
                             path={Routes.show_alert_condition(':streamId', ':conditionId')}
                             component={EditAlertConditionPage} />
                      <Route exact path={Routes.show_alert(':alertId')} component={ShowAlertPage} />

                      {!isCloud && <Route exact path={Routes.SYSTEM.INPUTS} component={InputsPage} />}
                      {!isCloud && <Route exact path={Routes.node_inputs(':nodeId')} component={NodeInputsPage} />}
                      {!isCloud && (
                      <Route exact path={Routes.global_input_extractors(':inputId')} component={ExtractorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.local_input_extractors(':nodeId', ':inputId')}
                             component={ExtractorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.new_extractor(':nodeId', ':inputId')}
                             component={CreateExtractorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.edit_extractor(':nodeId', ':inputId', ':extractorId')}
                             component={EditExtractorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.import_extractors(':nodeId', ':inputId')}
                             component={ImportExtractorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.export_extractors(':nodeId', ':inputId')}
                             component={ExportExtractorsPage} />
                      )}

                      <Route exact path={Routes.SYSTEM.CONFIGURATIONS} component={ConfigurationsPage} />

                      <Route exact path={Routes.SYSTEM.CONTENTPACKS.LIST} component={ContentPacksPage} />
                      <Route exact path={Routes.SYSTEM.CONTENTPACKS.CREATE} component={CreateContentPackPage} />
                      <Route exact
                             path={Routes.SYSTEM.CONTENTPACKS.edit(':contentPackId', ':contentPackRev')}
                             component={EditContentPackPage} />
                      <Route exact
                             path={Routes.SYSTEM.CONTENTPACKS.show(':contentPackId')}
                             component={ShowContentPackPage} />

                      <Route exact path={Routes.SYSTEM.GROKPATTERNS} component={GrokPatternsPage} />

                      <Route exact path={Routes.SYSTEM.INDEX_SETS.CREATE} component={IndexSetCreationPage} />
                      <Route exact path={Routes.SYSTEM.INDEX_SETS.SHOW(':indexSetId')} component={IndexSetPage} />
                      <Route exact
                             path={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(':indexSetId')}
                             component={IndexSetConfigurationPage} />

                      <Route exact path={Routes.SYSTEM.INDICES.LIST} component={IndicesPage} />
                      {!isCloud && (
                      <Route exact path={Routes.SYSTEM.INDICES.FAILURES} component={IndexerFailuresPage} />
                      )}

                      <Route exact path={Routes.SYSTEM.LOOKUPTABLES.OVERVIEW} component={LUTTablesPage} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.CREATE}
                             render={() => <LUTTablesPage action="create" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.show(':tableName')}
                             render={() => <LUTTablesPage action="show" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.edit(':tableName')}
                             render={() => <LUTTablesPage action="edit" />} />

                      <Route exact path={Routes.SYSTEM.LOOKUPTABLES.CACHES.OVERVIEW} component={LUTCachesPage} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.CACHES.CREATE}
                             render={() => <LUTCachesPage action="create" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(':cacheName')}
                             render={() => <LUTCachesPage action="show" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.CACHES.edit(':cacheName')}
                             render={() => <LUTCachesPage action="edit" />} />

                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.OVERVIEW}
                             component={LUTDataAdaptersPage} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.CREATE}
                             render={() => <LUTDataAdaptersPage action="create" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(':adapterName')}
                             render={() => <LUTDataAdaptersPage action="show" />} />
                      <Route exact
                             path={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.edit(':adapterName')}
                             render={() => <LUTDataAdaptersPage action="edit" />} />

                      <Route exact path={Routes.SYSTEM.PIPELINES.OVERVIEW} component={PipelinesOverviewPage} />
                      <Route exact path={Routes.SYSTEM.PIPELINES.RULES} component={RulesPage} />
                      <Route exact path={Routes.SYSTEM.PIPELINES.RULE(':ruleId')} component={RuleDetailsPage} />
                      <Route exact path={Routes.SYSTEM.PIPELINES.SIMULATOR} component={SimulatorPage} />
                      <Route exact path={Routes.SYSTEM.PIPELINES.PIPELINE(':pipelineId')} component={PipelineDetailsPage} />

                      {!isCloud && <Route exact path={Routes.SYSTEM.LOGGING} component={LoggersPage} />}
                      <Route exact path={Routes.SYSTEM.METRICS(':nodeId')} component={ShowMetricsPage} />
                      {!isCloud && <Route exact path={Routes.SYSTEM.NODES.LIST} component={NodesPage} />}
                      {!isCloud && <Route exact path={Routes.SYSTEM.NODES.SHOW(':nodeId')} component={ShowNodePage} />}

                      {!isCloud && <Route exact path={Routes.SYSTEM.OUTPUTS} component={SystemOutputsPage} />}

                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.ACTIVE}
                             component={AuthenticationPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE}
                             component={AuthenticationCreatePage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW}
                             component={AuthenticationOverviewPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.show(':backendId')}
                             component={AuthenticationBackendDetailsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(':backendId')}
                             component={AuthenticationBackendEditPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.BACKENDS.createBackend(':name')}
                             component={AuthenticationBackendCreatePage} />
                      )}

                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.AUTHENTICATORS.SHOW}
                             component={AuthenticatorsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.AUTHENTICATION.AUTHENTICATORS.EDIT}
                             component={AuthenticatorsEditPage} />
                      )}

                      <Route exact path={Routes.SYSTEM.USERS.OVERVIEW} component={UsersOverviewPage} />
                      <Route exact path={Routes.SYSTEM.USERS.CREATE} component={UserCreatePage} />
                      <Route exact path={Routes.SYSTEM.USERS.show(':userId')} component={UserDetailsPage} />
                      <Route exact path={Routes.SYSTEM.USERS.edit(':userId')} component={UserEditPage} />
                      <Route exact path={Routes.SYSTEM.USERS.TOKENS.edit(':userId')} component={UserTokensEditPage} />

                      <Route exact path={Routes.SYSTEM.AUTHZROLES.OVERVIEW} component={RolesOverviewPage} />
                      <Route exact path={Routes.SYSTEM.AUTHZROLES.show(':roleId')} component={RoleDetailsPage} />
                      <Route exact path={Routes.SYSTEM.AUTHZROLES.edit(':roleId')} component={RoleEditPage} />

                      <Route exact path={Routes.SYSTEM.OVERVIEW} component={SystemOverviewPage} />
                      <Route exact path={Routes.SYSTEM.PROCESSBUFFERDUMP(':nodeId')} component={ProcessBufferDumpPage} />
                      <Route exact path={Routes.SYSTEM.THREADDUMP(':nodeId')} component={ThreadDumpPage} />
                      <Route exact path={Routes.SYSTEM.ENTERPRISE} component={EnterprisePage} />

                      {!isCloud && (
                      <Route exact path={Routes.SYSTEM.SIDECARS.OVERVIEW} component={SidecarsPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.STATUS(':sidecarId')}
                             component={SidecarStatusPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.ADMINISTRATION}
                             component={SidecarAdministrationPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.CONFIGURATION}
                             component={SidecarConfigurationPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.NEW_CONFIGURATION}
                             component={SidecarNewConfigurationPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.EDIT_CONFIGURATION(':configurationId')}
                             component={SidecarEditConfigurationPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.NEW_COLLECTOR}
                             component={SidecarNewCollectorPage} />
                      )}
                      {!isCloud && (
                      <Route exact
                             path={Routes.SYSTEM.SIDECARS.EDIT_COLLECTOR(':collectorId')}
                             component={SidecarEditCollectorPage} />
                      )}
                      {standardPluginRoutes}
                      <Route path="*" render={() => <NotFoundPage displayPageLayout={false} />} />
                    </Switch>
                  </PageContentLayout>
                </Route>
                <Route exact path={Routes.NOTFOUND} component={NotFoundPage} />
              </Switch>
              <Route exact path={Routes.NOTFOUND} component={NotFoundPage} />
            </App>
          </Route>
        </Switch>
      </RouterErrorBoundary>
    </Router>
  );
};

export default AppRouter;
