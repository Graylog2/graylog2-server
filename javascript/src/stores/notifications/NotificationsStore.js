import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import NotificationsAction from 'actions/notifications/NotificationsActions';

const NotificationsStore = Reflux.createStore({
  listenables: [NotificationsAction],
  notifications: undefined,

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
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.NotificationsApiController.list().url);
    const promise = fetch('GET', url);

    NotificationsAction.list.promise(promise);
  },
  listCompleted(response) {
    this.notifications = response;
    this.trigger(response);
  },
  delete(notificationId) {

  },
});

export default NotificationsStore;
