import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import NotificationsActions from 'actions/notifications/NotificationsActions';

const NotificationsStore = Reflux.createStore({
  listenables: [NotificationsActions],
  notifications: undefined,
  POLL_INTERVAL: 3000,

  init() {
    this.list();
    setInterval(this.list, this.POLL_INTERVAL);
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

    NotificationsActions.list.promise(promise);
  },
  listCompleted(response) {
    this.notifications = response;
    this.trigger(response);
  },
  delete(type) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.NotificationsApiController.delete(type).url);
    const promise = fetch('DELETE', url);

    NotificationsActions.delete.promise(promise);
  },
  deleteCompleted() {
    this.list();
  },
});

export default NotificationsStore;
