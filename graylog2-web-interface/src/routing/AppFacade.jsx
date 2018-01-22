import React from 'react';
import PropTypes from 'prop-types';
import Reflux from 'reflux';
import { inject, observer } from 'mobx-react';
import LoginPage from 'react-proxy?name=LoginPage!pages/LoginPage';
import LoadingPage from 'react-proxy?name=LoadingPage!pages/LoadingPage';
import LoggedInPage from 'react-proxy?name=LoggedInPage!pages/LoggedInPage';
import ServerUnavailablePage from 'pages/ServerUnavailablePage';

import StoreProvider from 'injection/StoreProvider';

const SessionStore = StoreProvider.getStore('Session');
const ServerAvailabilityStore = StoreProvider.getStore('ServerAvailability');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import 'bootstrap/less/bootstrap.less';
import 'font-awesome/css/font-awesome.css';
import 'opensans-npm-webfont';
import 'stylesheets/bootstrap-submenus.less';
import 'toastr/toastr.less';
import 'rickshaw/rickshaw.css';
import 'stylesheets/graylog2.less';

const AppFacade = React.createClass({
  mixins: [Reflux.connect(ServerAvailabilityStore)],
  propTypes: {
    currentUser: PropTypes.object,
    sessionId: PropTypes.string,
  },

  componentDidMount() {
    this.interval = setInterval(ServerAvailabilityStore.ping, 20000);
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  render() {
    if (!this.state.server.up) {
      return <ServerUnavailablePage server={this.state.server} />;
    }
    if (!this.props.sessionId) {
      return <LoginPage />;
    }
    if (!this.props.currentUser) {
      return <LoadingPage text="We are preparing Graylog for you..." />;
    }
    return <LoggedInPage />;
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
  sessionId: SessionStore.sessionId,
}))(observer(AppFacade));
