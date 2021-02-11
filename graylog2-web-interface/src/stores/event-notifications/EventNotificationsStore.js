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
import URI from 'urijs';
import lodash from 'lodash';

import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');

const EventNotificationsStore = Reflux.createStore({
  listenables: [EventNotificationsActions],
  sourceUrl: '/events/notifications',
  all: undefined,
  allLegacyTypes: undefined,
  notifications: undefined,
  query: undefined,
  pagination: {
    count: undefined,
    page: undefined,
    pageSize: undefined,
    total: undefined,
    grandTotal: undefined,
  },

  getInitialState() {
    return this.getState();
  },

  propagateChanges() {
    this.trigger(this.getState());
  },

  getState() {
    return {
      all: this.all,
      allLegacyTypes: this.allLegacyTypes,
      notifications: this.notifications,
      query: this.query,
      pagination: this.pagination,
    };
  },

  eventNotificationsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);

    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  refresh() {
    if (this.all) {
      this.listAll();
    }

    if (this.pagination.page) {
      this.listPaginated({
        query: this.query,
        page: this.pagination.page,
        pageSize: this.pagination.pageSize,
      });
    }
  },

  listAll() {
    const promise = fetch('GET', this.eventNotificationsUrl({ query: { per_page: 0 } }));

    promise.then((response) => {
      this.all = response.notifications;
      this.propagateChanges();

      return response;
    });

    EventNotificationsActions.listAll.promise(promise);
  },

  listPaginated({ query = '', page = 1, pageSize = 10 }) {
    const promise = fetch('GET', this.eventNotificationsUrl({
      query: {
        query: query,
        page: page,
        per_page: pageSize,
      },
    }));

    promise.then((response) => {
      this.notifications = response.notifications;
      this.query = response.query;

      this.pagination = {
        count: response.count,
        page: response.page,
        pageSize: response.per_page,
        total: response.total,
        grandTotal: response.grand_total,
      };

      this.propagateChanges();

      return response;
    });

    EventNotificationsActions.listPaginated.promise(promise);
  },

  get(notificationId) {
    const promise = fetch('GET', this.eventNotificationsUrl({ segments: [notificationId] }));

    promise.catch((error) => {
      if (error.status === 404) {
        UserNotification.error(`Unable to find Event Notification with id <${notificationId}>, please ensure it wasn't deleted.`,
          'Could not retrieve Event Notification');
      }
    });

    EventNotificationsActions.get.promise(promise);
  },

  create(notification) {
    const promise = fetch('POST', this.eventNotificationsUrl({}), notification);

    promise.then(
      (response) => {
        UserNotification.success('Notification created successfully', `Notification "${notification.title}" was created successfully.`);
        this.refresh();

        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Creating Notification "${notification.title}" failed with status: ${error}`,
            'Could not save Notification');
        }
      },
    );

    EventNotificationsActions.create.promise(promise);
  },

  update(notificationId, notification) {
    const promise = fetch('PUT', this.eventNotificationsUrl({ segments: [notificationId] }), notification);

    promise.then(
      (response) => {
        UserNotification.success('Notification updated successfully', `Notification "${notification.title}" was updated successfully.`);
        this.refresh();

        return response;
      },
      (error) => {
        if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
          UserNotification.error(`Updating Notification "${notification.title}" failed with status: ${error}`,
            'Could not update Notification');
        }
      },
    );

    EventNotificationsActions.update.promise(promise);
  },

  delete(notification) {
    const promise = fetch('DELETE', this.eventNotificationsUrl({ segments: [notification.id] }));

    promise.then(
      () => {
        UserNotification.success('Notification deleted successfully', `Notification "${notification.title}" was deleted successfully.`);
        this.refresh();
      },
      (error) => {
        UserNotification.error(`Deleting Notification "${notification.title}" failed with status: ${error}`,
          'Could not delete Notification');
      },
    );

    EventNotificationsActions.delete.promise(promise);
  },

  test(notification) {
    const promise = fetch('POST', this.eventNotificationsUrl({ segments: ['test'] }), notification);

    EventNotificationsActions.test.promise(promise);
  },

  testPersisted(notification) {
    const promise = fetch('POST', this.eventNotificationsUrl({ segments: [notification.id, 'test'] }));

    EventNotificationsActions.testPersisted.promise(promise);
  },

  listAllLegacyTypes() {
    const promise = fetch('GET', this.eventNotificationsUrl({ segments: ['legacy', 'types'] }));

    promise.then((response) => {
      this.allLegacyTypes = response.types;
      this.propagateChanges();

      return response;
    });

    EventNotificationsActions.listAllLegacyTypes.promise(promise);
  },
});

export default EventNotificationsStore;
