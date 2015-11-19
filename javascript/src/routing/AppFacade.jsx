import React from 'react';
import Reflux from 'reflux';
import LoginPage from 'react-proxy?name=LoginPage!pages/LoginPage';
import LoggedInPage from 'react-proxy?name=LoggedInPage!pages/LoggedInPage';
import SessionStore from 'stores/sessions/SessionStore';

import 'javascripts/shims/styles/shim.css';
import 'stylesheets/bootstrap.min.css';
import 'stylesheets/font-awesome.min.css';
import 'stylesheets/newfonts.less';
import 'stylesheets/bootstrap-submenus.less';
import 'stylesheets/toastr.min.css';
import 'stylesheets/graylog2.less';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(SessionStore)],

  render() {
    if (!this.state.sessionId) {
      return <LoginPage />;
    } else {
      return <LoggedInPage />;
    }
  },
});

export default AppFacade;
