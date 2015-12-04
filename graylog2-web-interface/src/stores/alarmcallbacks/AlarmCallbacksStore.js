import Reflux from 'reflux';

import AlarmCallbacksActions from 'actions/alarmcallbacks/AlarmCallbacksActions';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const AlarmCallbacksStore = Reflux.createStore({
  listenables: [AlarmCallbacksActions],
  types: undefined,

  init() {
    this.available(undefined).then((types) => {
      this.types = types;
      this.trigger({types: types});
    });
  },

  getInitialState() {
    return {
      types: this.types,
    };
  },

  available(streamId) {
    const failCallback = (error) => {
      UserNotification.error('Fetching available AlarmCallback types failed with status: ' + error.message,
        'Could not retrieve available AlarmCallbacks');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbacksApiController.available(streamId).url);
    const promise = fetch('GET', url).then((response) => {
      return response.types;
    }, failCallback);

    AlarmCallbacksActions.available.promise(promise);

    return promise;
  },
  list(streamId) {
    const failCallback = (error) => {
      UserNotification.error('Fetching AlarmCallbacks failed with status: ' + error.message,
        'Could not retrieve AlarmCallbacks');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbacksApiController.list(streamId).url);
    const promise = fetch('GET', url).then((response) => response.alarmcallbacks, failCallback);

    AlarmCallbacksActions.list.promise(promise);
  },
  save(streamId, alarmCallback) {
    const failCallback = (error) => {
      UserNotification.error('Saving AlarmCallback failed with status: ' + error.message,
        'Could not save AlarmCallback');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbacksApiController.create(streamId).url);

    const promise = fetch('POST', url, alarmCallback).catch(failCallback);

    AlarmCallbacksActions.save.promise(promise);
  },
  delete(streamId, alarmCallbackId) {
    const failCallback = (error) => {
      UserNotification.error('Removing AlarmCallback failed with status: ' + error.message,
        'Could not remove AlarmCallback');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbacksApiController.delete(streamId, alarmCallbackId).url);

    const promise = fetch('DELETE', url).catch(failCallback);

    AlarmCallbacksActions.delete.promise(promise);
  },
  update(streamId, alarmCallbackId, deltas) {
    const failCallback = (error) => {
      UserNotification.error('Updating Alarm Callback \'' + alarmCallbackId + '\' failed with status: ' + error.message,
        'Could not update Alarm Callback');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.AlarmCallbacksApiController.update(streamId, alarmCallbackId).url);

    const promise = fetch('PUT', url, deltas).catch(failCallback);

    AlarmCallbacksActions.update.promise(promise);
  },
});

export default AlarmCallbacksStore;
