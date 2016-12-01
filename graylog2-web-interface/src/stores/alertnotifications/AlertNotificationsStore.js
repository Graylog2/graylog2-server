import Reflux from 'reflux';

import ActionsProvider from 'injection/ActionsProvider';
const AlertNotificationsActions = ActionsProvider.getActions('AlertNotifications');

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

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
        response => {
          this.availableNotifications = response.types;
          this.trigger({ availableNotifications: this.availableNotifications });
          return this.availableNotifications;
        },
        error => {
          UserNotification.error(`Fetching available alert notification types failed with status: ${error.message}`,
            'Could not retrieve available alert notifications');
        });

    AlertNotificationsActions.available.promise(promise);
  },
  listAll() {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.listAll().url);
    const promise = fetch('GET', url);
    promise.then(
      response => {
        this.allNotifications = response.alarmcallbacks;
        this.trigger({ allNotifications: this.allNotifications });
        return this.allNotifications;
      },
      (error) => {
        UserNotification.error(`Fetching alert notifications failed with status: ${error.message}`,
          'Could not retrieve alert notifications');
      });

    AlertNotificationsActions.listAll.promise(promise);
  },
});

export default AlertNotificationsStore;
