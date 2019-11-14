import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import loadAsync from 'routing/loadAsync';

import 'bootstrap/less/bootstrap.less';
import 'opensans-npm-webfont';
import 'stylesheets/bootstrap-submenus.less';
import 'toastr/toastr.less';
import 'rickshaw/rickshaw.css';
import 'stylesheets/graylog2.less';

import ServerUnavailablePage from 'pages/ServerUnavailablePage';
import StoreProvider from 'injection/StoreProvider';

import GraylogThemeProvider from '../theme/GraylogThemeProvider';

const SessionStore = StoreProvider.getStore('Session');
const ServerAvailabilityStore = StoreProvider.getStore('ServerAvailability');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const LoginPage = loadAsync(() => import(/* webpackChunkName: "LoginPage" */ 'pages/LoginPage'));
const LoadingPage = loadAsync(() => import(/* webpackChunkName: "LoadingPage" */ 'pages/LoadingPage'));
const LoggedInPage = loadAsync(() => import(/* webpackChunkName: "LoggedInPage" */ 'pages/LoggedInPage'));

const AppFacade = createReactClass({
  displayName: 'AppFacade',
  mixins: [Reflux.connect(SessionStore), Reflux.connect(ServerAvailabilityStore), Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    this.interval = setInterval(ServerAvailabilityStore.ping, 20000);
  },

  componentWillUnmount() {
    if (this.interval) {
      clearInterval(this.interval);
    }
  },

  render() {
    const { currentUser, server, sessionId } = this.state;
    let Page = <LoggedInPage />;

    if (!server.up) {
      Page = <ServerUnavailablePage server={server} />;
    } else if (!sessionId) {
      Page = <LoginPage />;
    } else if (!currentUser) {
      Page = <LoadingPage text="We are preparing Graylog for you..." />;
    }

    return (
      <GraylogThemeProvider>
        {Page}
      </GraylogThemeProvider>
    );
  },
});

export default AppFacade;
