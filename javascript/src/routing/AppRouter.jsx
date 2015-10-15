import React from 'react';
import App from 'routing/App';
import { Router, Route } from 'react-router';

import Routes from 'routing/Routes';
import DebugHandler from './DebugHandler';

import StreamsPage from 'components/streams/StreamsPage';
import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import StreamOutputsPage from 'pages/StreamOutputsPage';
import DashboardsPage from 'components/dashboard/DashboardsPage';
import ShowDashboardPage from 'components/dashboard/ShowDashboardPage';

const AppRouter = React.createClass({
  render() {
    return (
      <Router>
        <Route path="/" component={App}>
          <Route path={Routes.STREAMS} component={StreamsPage}/>
          <Route path={Routes.stream_edit(':streamId')} component={StreamRulesEditor}/>
          <Route path={Routes.stream_outputs(':streamId')} component={StreamOutputsPage}/>
          <Route path={Routes.DASHBOARDS} component={DashboardsPage}/>
          <Route path={Routes.dashboard_show(':dashboardId')} component={ShowDashboardPage}/>
          <Route path="system">
            <Route path="nodes" component={DebugHandler}/>
          </Route>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
