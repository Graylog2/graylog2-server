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
import 'stylesheets/rickshaw.min.css';
import 'stylesheets/graylog2.less';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(SessionStore)],

  childContextTypes: {
    storeProvider: React.PropTypes.object,
    actionsProvider: React.PropTypes.object,
  },

  getChildContext: function() {
    return {
      storeProvider: this.props.storeProvider,
      actionsProvider: this.props.actionsProvider,
    };
  },

  render() {
    if (!this.state.sessionId) {
      return <LoginPage />;
    } else {
      return <LoggedInPage />;
    }
  },
});

export default AppFacade;
