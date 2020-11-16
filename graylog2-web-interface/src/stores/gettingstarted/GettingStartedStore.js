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
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';

const GettingStartedActions = ActionsProvider.getActions('GettingStarted');

const GettingStartedStore = Reflux.createStore({
  listenables: [GettingStartedActions],
  sourceUrl: '/system/gettingstarted',
  status: undefined,

  init() {
    this.getStatus();
  },

  getInitialState() {
    return { status: this.status };
  },

  get() {
    return this.status;
  },

  getStatus() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));

    promise
      .then(
        (response) => {
          this.status = response;
          this.trigger({ status: this.status });

          return response;
        },
        (error) => console.error(error),
      );

    GettingStartedActions.getStatus.promise(promise);
  },

  dismiss() {
    const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/dismiss`), '{}');

    promise
      .then(
        (response) => {
          this.getStatus();

          return response;
        },
        (error) => {
          UserNotification.error(`Dismissing Getting Started Guide failed with status: ${error}`,
            'Could not dismiss guide');
        },
      );

    GettingStartedActions.dismiss.promise(promise);
  },
});

export default GettingStartedStore;
