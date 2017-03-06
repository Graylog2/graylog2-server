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
