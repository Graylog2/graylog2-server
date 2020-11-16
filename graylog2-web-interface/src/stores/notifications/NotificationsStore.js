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

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { Builder, fetchPeriodically } from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const NotificationsActions = ActionsProvider.getActions('Notifications');

const NotificationsStore = Reflux.createStore({
  listenables: [NotificationsActions],
  notifications: undefined,
  promises: {},

  init() {
    this.list();
  },
  getInitialState() {
    if (this.notifications) {
      return this.notifications;
    }

    return {};
  },
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.list().url);
    const promise = this.promises.list || fetchPeriodically('GET', url)
      .finally(() => delete this.promises.list);

    this.promises.list = promise;

    NotificationsActions.list.promise(promise);
  },
  listCompleted(response) {
    this.notifications = response;
    this.trigger(response);
  },
  delete(type) {
    const url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.delete(type).url);
    const promise = fetch('DELETE', url);

    NotificationsActions.delete.promise(promise);
  },
  deleteCompleted() {
    this.list();
  },
});

export default NotificationsStore;
