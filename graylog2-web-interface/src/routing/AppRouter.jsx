import React from 'react';
import App from 'routing/App';
import AppWithSearchBar from 'routing/AppWithSearchBar';
import AppWithoutSearchBar from 'routing/AppWithoutSearchBar';
import { IndexRoute, Redirect, Router, Route } from 'react-router';
import history from 'util/History';
import { PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'routing/Routes';

import StartPage from 'pages/StartPage';
import SearchPage from 'pages/SearchPage';
import ShowMessagePage from 'pages/ShowMessagePage';
import StreamsPage from 'pages/StreamsPage';
import StreamEditPage from 'pages/StreamEditPage';
import StreamOutputsPage from 'pages/StreamOutputsPage';
import StreamAlertsPage from 'pages/StreamAlertsPage';
import StreamSearchPage from 'pages/StreamSearchPage';
import DashboardsPage from 'pages/DashboardsPage';
import ShowDashboardPage from 'pages/ShowDashboardPage';
import SourcesPage from 'pages/SourcesPage';
import InputsPage from 'pages/InputsPage';
import NodeInputsPage from 'pages/NodeInputsPage';
import ExtractorsPage from 'pages/ExtractorsPage';
import CreateExtractorsPage from 'pages/CreateExtractorsPage';
import EditExtractorsPage from 'pages/EditExtractorsPage';
import ImportExtractorsPage from 'pages/ImportExtractorsPage';
import ExportExtractorsPage from 'pages/ExportExtractorsPage';
import SystemOutputsPage from 'pages/SystemOutputsPage';
import RolesPage from 'pages/RolesPage';
import ContentPacksPage from 'pages/ContentPacksPage';
import ExportContentPackPage from 'pages/ExportContentPackPage';
import UsersPage from 'pages/UsersPage';
import CreateUsersPage from 'pages/CreateUsersPage';
import EditUsersPage from 'pages/EditUsersPage';
import GrokPatternsPage from 'pages/GrokPatternsPage';
import SystemOverviewPage from 'pages/SystemOverviewPage';
import IndexerFailuresPage from 'pages/IndexerFailuresPage';
import IndicesPage from 'pages/IndicesPage';
import LoggersPage from 'pages/LoggersPage';
import GettingStartedPage from 'pages/GettingStartedPage';
import ShowMetricsPage from 'pages/ShowMetricsPage';
import ShowNodePage from 'pages/ShowNodePage';
import NodesPage from 'pages/NodesPage';
import ThreadDumpPage from 'pages/ThreadDumpPage';
import LdapPage from 'pages/LdapPage';
import LdapGroupsPage from 'pages/LdapGroupsPage';
import ConfigurationsPage from 'pages/ConfigurationsPage';
import NotFoundPage from 'pages/NotFoundPage';

const AppRouter = React.createClass({
  render() {
    const pluginRoutes = PluginStore.exports('routes').map((pluginRoute) => {
      return <Route key={pluginRoute.component.displayName} path={pluginRoute.path} component={pluginRoute.component} />;
    });
    return (
      <Router history={history}>
        <Route path="/" component={App}>
          <IndexRoute component={StartPage}/>
          <Route component={AppWithSearchBar}>
            <Route path={Routes.SEARCH} component={SearchPage}/>
            <Route path={Routes.message_show(':index', ':messageId')} component={ShowMessagePage}/>
            <Route path={Routes.SOURCES} component={SourcesPage}/>
            <Route path={Routes.stream_search(':streamId')} component={StreamSearchPage}/>
            <Redirect from={Routes.legacy_stream_search(':streamId')} to={Routes.stream_search(':streamId')} />
          </Route>
          <Route component={AppWithoutSearchBar}>
            <Route path={Routes.GETTING_STARTED} component={GettingStartedPage}/>
            <Route path={Routes.STREAMS} component={StreamsPage}/>
            <Route path={Routes.stream_edit(':streamId')} component={StreamEditPage}/>
            <Route path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage}/>
            <Route path={Routes.stream_alerts(':streamId')} component={StreamAlertsPage}/>
            <Route path={Routes.DASHBOARDS} component={DashboardsPage}/>
            <Route path={Routes.dashboard_show(':dashboardId')} component={ShowDashboardPage}/>
            <Route path={Routes.SYSTEM.INPUTS} component={InputsPage}/>
            <Route path={Routes.node_inputs(':nodeId')} component={NodeInputsPage}/>
            <Route path={Routes.global_input_extractors(':inputId')} component={ExtractorsPage}/>
            <Route path={Routes.local_input_extractors(':nodeId', ':inputId')} component={ExtractorsPage}/>
            <Route path={Routes.new_extractor(':nodeId', ':inputId')} component={CreateExtractorsPage}/>
            <Route path={Routes.edit_extractor(':nodeId', ':inputId', ':extractorId')} component={EditExtractorsPage}/>
            <Route path={Routes.import_extractors(':nodeId', ':inputId')} component={ImportExtractorsPage}/>
            <Route path={Routes.export_extractors(':nodeId', ':inputId')} component={ExportExtractorsPage}/>
            <Route path={Routes.SYSTEM.CONFIGURATIONS} component={ConfigurationsPage}/>
            <Route path={Routes.SYSTEM.CONTENTPACKS.LIST} component={ContentPacksPage}/>
            <Route path={Routes.SYSTEM.CONTENTPACKS.EXPORT} component={ExportContentPackPage}/>
            <Route path={Routes.SYSTEM.GROKPATTERNS} component={GrokPatternsPage}/>
            <Route path={Routes.SYSTEM.INDICES.LIST} component={IndicesPage}/>
            <Route path={Routes.SYSTEM.INDICES.FAILURES} component={IndexerFailuresPage}/>
            <Route path={Routes.SYSTEM.LOGGING} component={LoggersPage}/>
            <Route path={Routes.SYSTEM.METRICS(':nodeId')} component={ShowMetricsPage}/>
            <Route path={Routes.SYSTEM.NODES.LIST} component={NodesPage}/>
            <Route path={Routes.SYSTEM.NODES.SHOW(':nodeId')} component={ShowNodePage}/>
            <Route path={Routes.SYSTEM.OUTPUTS} component={SystemOutputsPage}/>
            <Route path={Routes.SYSTEM.ROLES} component={RolesPage}/>
            <Route path={Routes.SYSTEM.USERS.CREATE} component={CreateUsersPage}/>
            <Route path={Routes.SYSTEM.USERS.edit(':username')} component={EditUsersPage}/>
            <Route path={Routes.SYSTEM.USERS.LIST} component={UsersPage}/>
            <Route path={Routes.SYSTEM.OVERVIEW} component={SystemOverviewPage}/>
            <Route path={Routes.SYSTEM.THREADDUMP(':nodeId')} component={ThreadDumpPage}/>
            <Route path={Routes.SYSTEM.LDAP.SETTINGS} component={LdapPage}/>
            <Route path={Routes.SYSTEM.LDAP.GROUPS} component={LdapGroupsPage}/>
            {pluginRoutes}
            <Route path="*" component={NotFoundPage}/>
          </Route>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
