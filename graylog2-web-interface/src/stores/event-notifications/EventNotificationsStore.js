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

  getInitialState() {
    return {
      list: [],
    };
  },

  eventNotificationsUrl({ segments = [], query = {} }) {
    const uri = new URI(this.sourceUrl);
    const nextSegments = lodash.concat(uri.segment(), segments);
    uri.segmentCoded(nextSegments);
    uri.query(query);

    return URLUtils.qualifyUrl(uri.resource());
  },

  list() {
    // TODO: This needs to user proper pagination instead of requesting 1000 items
    const promise = fetch('GET', this.eventNotificationsUrl({ query: { per_page: 1000 } }));

    promise.then((response) => {
      this.trigger({ list: response.notifications });
      return response;
    });

    EventNotificationsActions.list.promise(promise);
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
        this.list();
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
        this.list();
      },
      (error) => {
        UserNotification.error(`Deleting Notification "${notification.title}" failed with status: ${error}`,
          'Could not delete Notification');
      },
    );

    EventNotificationsActions.delete.promise(promise);
  },
});

export default EventNotificationsStore;
