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

import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

export type NotificationType = {
  severity: string,
  type: string,
  key?: string,
  timestamp: string,
  node_id: string,
  details?: {
    [key: string]: string,
  },
};

export type NotificationMessage = {
  title: string,
  description: string,
};

type NotificationsStoreType = {
  notifications: Array<NotificationType>,
  total: number,
  messages:{
    [key: string]: NotificationMessage,
  },
};

type NotificationMessageOptions ={
  values?: {
    [key: string]: string,
  }
}
type NotificationsActionType = {
  delete: (type: string, key: string) => Promise<unknown>,
  list: () => Promise<unknown>,
  getHtmlMessage: (type: string, key: string, options: NotificationMessageOptions) => Promise<unknown>,
}
export const NotificationsActions = singletonActions(
  'core.Notifications',
  () => Reflux.createActions<NotificationsActionType>({
    delete: { asyncResult: true },
    list: { asyncResult: true },
    getHtmlMessage: { asyncResult: true },
  }),
);

export const NotificationsStore = singletonStore(
  'core.Notifications',
  () => Reflux.createStore<NotificationsStoreType>({
    listenables: [NotificationsActions],
    notifications: undefined,
    total: undefined,
    message: {},
    promises: {},

    init() {
      this.list();
    },
    getInitialState() {
      return {
        notifications: this.notifications,
        total: this.total,
        messages: this.messages,
      };
    },
    propagateChanges() {
      this.trigger({
        notifications: this.notifications,
        total: this.total,
        messages: this.messages,
      });
    },

    list() {
      const url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.list().url);
      const promise = this.promises.list || fetchPeriodically('GET', url)
        .finally(() => delete this.promises.list);

      this.promises.list = promise;

      NotificationsActions.list.promise(promise);
    },
    listCompleted(response) {
      this.notifications = response.notifications;
      this.total = response.total;
      this.propagateChanges();
    },
    delete(type: string, key: string) {
      let url;

      if (key) {
        url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.deleteWithKey(type, key).url);
      } else {
        url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.delete(type).url);
      }

      const promise = fetch('DELETE', url);

      NotificationsActions.delete.promise(promise);
    },
    deleteCompleted() {
      this.list();
      this.propagateChanges();
    },
    getHtmlMessage(type: string, key: string, options: NotificationMessageOptions = { values: {} }) {
      let url;

      if (key) {
        url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.getHtmlMessageWithKey(type, key).url);
      } else {
        url = URLUtils.qualifyUrl(ApiRoutes.NotificationsApiController.getHtmlMessage(type).url);
      }

      const promise = fetch('POST', url, options);

      promise.then((response) => {
        this.messages = { ...this.messages, [`${type}-${key}`]: response };
        this.propagateChanges();
      });

      NotificationsActions.getHtmlMessage.promise(promise);
    },
  }),
);
