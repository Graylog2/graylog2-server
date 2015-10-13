import React from 'react';
import App from 'routing/App';
import { Router, Route } from 'react-router';
import StreamsPage from 'components/streams/StreamsPage';
import DashboardsPage from 'components/dashboard/DashboardsPage';
import Routes from 'routing/Routes';
import DebugHandler from './DebugHandler';

const AppRouter = React.createClass({
  render() {
    return (
      <Router>
        <Route path="/" component={App}>
          <Route path={Routes.STREAMS} component={StreamsPage}/>
          <Route path={Routes.DASHBOARDS} component={DashboardsPage}/>
          <Route path="system">
            <Route path="nodes" component={DebugHandler}/>
          </Route>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
