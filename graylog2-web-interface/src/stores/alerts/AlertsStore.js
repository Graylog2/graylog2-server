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

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import ActionsProvider from 'injection/ActionsProvider';

const AlertsActions = ActionsProvider.getActions('Alerts');

const AlertsStore = Reflux.createStore({
  listenables: [AlertsActions],

  list(stream, since) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.list(stream.id, since).url);
    const promise = fetch('GET', url);

    promise
      .then(
        (response) => this.trigger({ alerts: response }),
        (error) => {
          UserNotification.error(`Fetching alerts for stream "${stream.title}" failed with status: ${error.message}`,
            `Could not retrieve alerts for stream "${stream.title}".`);
        },
      );

    AlertsActions.list.promise(promise);
  },

  listPaginated(streamId, skip, limit, state = 'any') {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.listPaginated(streamId, skip, limit, state).url);
    const promise = fetch('GET', url);

    promise
      .then(
        (response) => this.trigger({ alerts: response }),
        (error) => {
          UserNotification.error(`Fetching alerts failed with status: ${error.message}`, 'Could not retrieve alerts.');
        },
      );

    AlertsActions.listPaginated.promise(promise);
  },

  listAllPaginated(skip, limit, state) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.listAllPaginated(skip, limit, state).url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => this.trigger({ alerts: response }),
      (error) => {
        UserNotification.error(`Fetching alerts failed with status: ${error.message}`, 'Could not retrieve alerts.');
      },
    );

    AlertsActions.listAllPaginated.promise(promise);
  },

  listAllStreams(since) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.listAllStreams(since).url);
    const promise = fetch('GET', url);

    promise
      .then(
        (response) => this.trigger({ alerts: response }),
        (error) => {
          UserNotification.error(`Fetching alerts failed with status: ${error.message}`, 'Could not retrieve alerts.');
        },
      );

    AlertsActions.listAllStreams.promise(promise);
  },

  get(alertId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.get(alertId).url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => {
        this.trigger({ alert: response });

        return response;
      },
      (error) => {
        UserNotification.error(`Fetching alert '${alertId}' failed with status: ${error.message}`, 'Could not retrieve alert.');
      },
    );

    AlertsActions.get.promise(promise);
  },
});

export default AlertsStore;
