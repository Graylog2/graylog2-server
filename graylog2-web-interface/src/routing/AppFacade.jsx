import React, { useEffect } from 'react';
import PropTypes from 'prop-types';

import loadAsync from 'routing/loadAsync';
import ServerUnavailablePage from 'pages/ServerUnavailablePage';
import StoreProvider from 'injection/StoreProvider';
import connect from 'stores/connect';
import GraylogThemeProvider from 'theme/GraylogThemeContext';

import 'bootstrap/less/bootstrap.less';
import 'opensans-npm-webfont';
import 'stylesheets/bootstrap-submenus.less';
import 'toastr/toastr.less';
import 'rickshaw/rickshaw.css';
import 'stylesheets/graylog2.less';

const SessionStore = StoreProvider.getStore('Session');
const ServerAvailabilityStore = StoreProvider.getStore('ServerAvailability');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const LoginPage = loadAsync(() => import(/* webpackChunkName: "LoginPage" */ 'pages/LoginPage'));
const LoadingPage = loadAsync(() => import(/* webpackChunkName: "LoadingPage" */ 'pages/LoadingPage'));
const LoggedInPage = loadAsync(() => import(/* webpackChunkName: "LoggedInPage" */ 'pages/LoggedInPage'));

const SERVER_PING_TIMEOUT = 20000;

export const AppFacade = ({ currentUser, server, sessionId }) => {
  let interval;
  let Page;

  useEffect(() => {
    interval = setInterval(ServerAvailabilityStore.ping, SERVER_PING_TIMEOUT);
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, []);

  if (!server.up) {
    Page = <ServerUnavailablePage server={server} />;
  } else if (!sessionId) {
    Page = <LoginPage />;
  } else if (!currentUser) {
    Page = <LoadingPage text="We are preparing Graylog for you..." />;
  } else {
    Page = <LoggedInPage />;
  }

  return (
    <GraylogThemeProvider>
      {Page}
    </GraylogThemeProvider>
  );
};

AppFacade.propTypes = {
  currentUser: PropTypes.object,
  server: PropTypes.shape({
    up: PropTypes.bool,
  }),
  sessionId: PropTypes.string,
};

AppFacade.defaultProps = {
  currentUser: undefined,
  server: undefined,
  sessionId: undefined,
};

export default connect(AppFacade, {
  currentUser: CurrentUserStore,
  server: ServerAvailabilityStore,
  sessionId: SessionStore,
}, ({
  currentUser: { currentUser } = {},
  server: { server } = {},
  sessionId: { sessionId } = '',
}) => ({ currentUser, server, sessionId }));
