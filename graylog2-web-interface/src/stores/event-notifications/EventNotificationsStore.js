import Reflux from 'reflux';
import URI from 'urijs';
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');

const EventNotificationsStore = Reflux.createStore({
  listenables: [EventNotificationsActions],
  sourceUrl: '/plugins/org.graylog.events/notifications',
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
        UserNotification.error(`Creating Notification "${notification.title}" failed with status: ${error}`,
          'Could not save Notification');
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
        UserNotification.error(`Updating Notification "${notification.title}" failed with status: ${error}`,
          'Could not update Notification');
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

  listAllLegacyTypes() {
    const promise = fetch('GET', this.eventNotificationsUrl({ segments: ['legacy', 'types'] }));

    promise.then((response) => {
      console.log('HERE', response);
      this.allLegacyTypes = response.types;
      this.propagateChanges();
      return response;
    });

    EventNotificationsActions.listAllLegacyTypes.promise(promise);
  },
});

export default EventNotificationsStore;
