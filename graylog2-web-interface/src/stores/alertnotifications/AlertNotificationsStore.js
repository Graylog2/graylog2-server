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

import ActionsProvider from 'injection/ActionsProvider';
import UserNotification from 'util/UserNotification';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const AlertNotificationsActions = ActionsProvider.getActions('AlertNotifications');

const AlertNotificationsStore = Reflux.createStore({
  listenables: [AlertNotificationsActions],
  availableNotifications: undefined,
  allNotifications: undefined,

  getInitialState() {
    return {
      availableNotifications: this.availableNotifications,
      allNotifications: this.allNotifications,
    };
  },

  available() {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.available().url);
    const promise = fetch('GET', url);

    promise
      .then(
        (response) => {
          this.availableNotifications = response.types;
          this.trigger({ availableNotifications: this.availableNotifications });

          return this.availableNotifications;
        },
        (error) => {
          UserNotification.error(`Fetching available alert notification types failed with status: ${error.message}`,
            'Could not retrieve available alert notifications');
        },
      );

    AlertNotificationsActions.available.promise(promise);
  },

  listAll() {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.listAll().url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => {
        this.allNotifications = response.alarmcallbacks;
        this.trigger({ allNotifications: this.allNotifications });

        return this.allNotifications;
      },
      (error) => {
        UserNotification.error(`Fetching alert notifications failed with status: ${error.message}`,
          'Could not retrieve alert notifications');
      },
    );

    AlertNotificationsActions.listAll.promise(promise);
  },

  testAlert(alarmCallbackId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.testAlert(alarmCallbackId).url);

    const promise = fetch('POST', url);

    promise.then(
      () => UserNotification.success('Test notification was sent successfully'),
      (error) => {
        const message = (error.additional && error.additional.body && error.additional.body.message ? error.additional.body.message : error.message);

        UserNotification.error(`Sending test alert notification failed with message: ${message}`,
          'Could not send test alert notification');
      },
    );

    AlertNotificationsActions.testAlert.promise(promise);

    // Need to do this to handle possible concurrent calls to this method
    return promise;
  },
});

export default AlertNotificationsStore;
