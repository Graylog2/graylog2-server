import React from 'react';
import App from 'routing/App';
import { Router, Route } from 'react-router';
import StreamsPage from 'components/streams/StreamsPage';
import Routes from 'routing/Routes';
import LoginPage from 'components/sessions/LoginPage';

const AppRouter = React.createClass({
  render() {
    return (
      <Router>
        <Route path="/" component={App}>
          <Route path={Routes.STREAMS} component={StreamsPage}/>
        </Route>
      </Router>
    );
  },
});

export default AppRouter;
