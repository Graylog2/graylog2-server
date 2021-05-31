/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';

import loadAsync from 'routing/loadAsync';
import ServerUnavailablePage from 'pages/ServerUnavailablePage';
import StoreProvider from 'injection/StoreProvider';
import connect from 'stores/connect';
import LoginQueryClientProvider from 'contexts/LoginQueryClientProvider';

import 'bootstrap/less/bootstrap.less';
import 'toastr/toastr.less';

const SessionStore = StoreProvider.getStore('Session');
const ServerAvailabilityStore = StoreProvider.getStore('ServerAvailability');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const LoginPage = loadAsync(() => import(/* webpackChunkName: "LoginPage" */ 'pages/LoginPage'));
const LoadingPage = loadAsync(() => import(/* webpackChunkName: "LoadingPage" */ 'pages/LoadingPage'));
const LoggedInPage = loadAsync(() => import(/* webpackChunkName: "LoggedInPage" */ 'pages/LoggedInPage'));

const SERVER_PING_TIMEOUT = 20000;

const AppFacade = ({ currentUser, server, sessionId }) => {
  useEffect(() => {
    const interval = setInterval(ServerAvailabilityStore.ping, SERVER_PING_TIMEOUT);

    return () => clearInterval(interval);
  }, []);

  if (!server.up) {
    return <ServerUnavailablePage server={server} />;
  }

  if (!sessionId) {
    return (
      <LoginQueryClientProvider>
        <LoginPage />
      </LoginQueryClientProvider>
    );
  }

  if (!currentUser) {
    return <LoadingPage text="We are preparing Graylog for you..." />;
  }

  return <LoggedInPage />;
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
