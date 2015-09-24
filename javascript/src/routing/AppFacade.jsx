import React from 'react';
import Reflux from 'reflux';
import AppRouter from 'routing/AppRouter';
import LoginPage from 'components/sessions/LoginPage';
import SessionStore from 'stores/sessions/SessionStore';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(SessionStore)],

  render() {
    if (!SessionStore.isLoggedIn()) {
      return <LoginPage />;
    } else {
      return <AppRouter />;
    }
  },
});

export default AppFacade;
