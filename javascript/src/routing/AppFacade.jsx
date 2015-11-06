import React from 'react';
import Reflux from 'reflux';
import AppRouter from 'routing/AppRouter';
import LoginPage from 'pages/LoginPage';
import SessionStore from 'stores/sessions/SessionStore';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(SessionStore)],

  render() {
    if (!this.state.sessionId) {
      return <LoginPage />;
    } else {
      return <AppRouter />;
    }
  },
});

export default AppFacade;
