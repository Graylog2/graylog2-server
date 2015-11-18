import React from 'react';
import App from 'routing/App';
import AppWithSearchBar from 'routing/AppWithSearchBar';
import AppWithoutSearchBar from 'routing/AppWithoutSearchBar';
import { IndexRoute, Router, Route } from 'react-router';
import {createHistory} from 'history';

import Routes from 'routing/Routes';

import SearchPage from 'pages/SearchPage';
import ShowMessagePage from 'pages/ShowMessagePage';
import StreamsPage from 'pages/StreamsPage';
import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import StreamOutputsPage from 'pages/StreamOutputsPage';
import StreamAlertsPage from 'pages/StreamAlertsPage';
import DashboardsPage from 'pages/DashboardsPage';
import ShowDashboardPage from 'pages/ShowDashboardPage';
import SourcesPage from 'pages/SourcesPage';
import InputsPage from 'pages/InputsPage';
import ExtractorsPage from 'pages/ExtractorsPage';
import CreateExtractorsPage from 'pages/CreateExtractorsPage';
import EditExtractorsPage from 'pages/EditExtractorsPage';
import ImportExtractorsPage from 'pages/ImportExtractorsPage';
import ExportExtractorsPage from 'pages/ExportExtractorsPage';
import CollectorsPage from 'pages/CollectorsPage';
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

const AppRouter = React.createClass({
  render() {
    return (
      <Router history={createHistory()}>
        <Route path="/" component={App}>
          <Route component={AppWithSearchBar}>
            <IndexRoute component={SearchPage}/>
            <Route path={Routes.SEARCH} component={SearchPage}/>
            <Route path={Routes.message_show(':index', ':messageId')} component={ShowMessagePage}/>
            <Route path={Routes.SOURCES} component={SourcesPage}/>
          </Route>
          <Route component={AppWithoutSearchBar}>
            <Route path={Routes.STREAMS} component={StreamsPage}/>
            <Route path={Routes.stream_edit(':streamId')} component={StreamRulesEditor}/>
            <Route path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage}/>
            <Route path={Routes.stream_alerts(':streamId')} component={StreamAlertsPage}/>
            <Route path={Routes.DASHBOARDS} component={DashboardsPage}/>
            <Route path={Routes.dashboard_show(':dashboardId')} component={ShowDashboardPage}/>
            <Route path={Routes.SYSTEM.INPUTS} component={InputsPage}/>
            <Route path={Routes.global_input_extractors(':inputId')} component={ExtractorsPage}/>
            <Route path={Routes.local_input_extractors(':nodeId', ':inputId')} component={ExtractorsPage}/>
            <Route path={Routes.new_extractor(':nodeId', ':inputId')} component={CreateExtractorsPage}/>
            <Route path={Routes.edit_extractor(':nodeId', ':inputId', ':extractorId')} component={EditExtractorsPage}/>
            <Route path={Routes.import_extractors(':nodeId', ':inputId')} component={ImportExtractorsPage}/>
            <Route path={Routes.export_extractors(':nodeId', ':inputId')} component={ExportExtractorsPage}/>
            <Route path={Routes.SYSTEM.COLLECTORS} component={CollectorsPage}/>
            <Route path={Routes.SYSTEM.CONTENTPACKS.LIST} component={ContentPacksPage}/>
            <Route path={Routes.SYSTEM.CONTENTPACKS.EXPORT} component={ExportContentPackPage}/>
            <Route path={Routes.SYSTEM.GROKPATTERNS} component={GrokPatternsPage}/>
            <Route path={Routes.SYSTEM.INDICES.LIST} component={IndicesPage}/>
            <Route path={Routes.SYSTEM.INDICES.FAILURES} component={IndexerFailuresPage}/>
            <Route path={Routes.SYSTEM.LOGGING} component={LoggersPage}/>
            <Route path={Routes.SYSTEM.OUTPUTS} component={SystemOutputsPage}/>
            <Route path={Routes.SYSTEM.ROLES} component={RolesPage}/>
            <Route path={Routes.SYSTEM.USERS.CREATE} component={CreateUsersPage}/>
            <Route path={Routes.SYSTEM.USERS.edit(':username')} component={EditUsersPage}/>
            <Route path={Routes.SYSTEM.USERS.LIST} component={UsersPage}/>
            <Route path={Routes.SYSTEM.OVERVIEW} component={SystemOverviewPage}/>
          </Route>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
