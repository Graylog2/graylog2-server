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
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import Routes from 'routing/Routes';
import history from 'util/History';
import PermissionsMixin from 'util/PermissionsMixin';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const GettingStartedStore = StoreProvider.getStore('GettingStarted');
const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const StartPage = createReactClass({
  displayName: 'StartPage',
  mixins: [Reflux.connect(CurrentUserStore), Reflux.listenTo(GettingStartedStore, 'onGettingStartedUpdate')],

  getInitialState() {
    return {
      gettingStarted: undefined,
    };
  },

  componentDidMount() {
    GettingStartedActions.getStatus();
    CurrentUserStore.reload();
  },

  componentDidUpdate() {
    if (!this._isLoading()) {
      this._redirectToStartpage();
    }
  },

  onGettingStartedUpdate(state) {
    this.setState({ gettingStarted: state.status });
  },

  _redirect(page) {
    history.replace(page);
  },

  _redirectToStartpage() {
    const { currentUser: { startpage, permissions }, gettingStarted } = this.state;

    // Show getting started page if user is an admin and getting started wasn't dismissed
    if (PermissionsMixin.isPermitted(permissions, ['inputs:create'])) {
      if (gettingStarted.show) {
        this._redirect(Routes.GETTING_STARTED);

        return;
      }
    }

    // Show custom startpage if it was set
    if (startpage !== null && Object.keys(startpage).length > 0) {
      if (startpage.type === 'stream') {
        this._redirect(Routes.stream_search(startpage.id));
      } else {
        this._redirect(Routes.dashboard_show(startpage.id));
      }

      return;
    }

    this._redirect(Routes.SEARCH);
  },

  _isLoading() {
    const { currentUser, gettingStarted } = this.state;

    return !currentUser || !gettingStarted;
  },

  render() {
    return <Spinner />;
  },
});

export default StartPage;
