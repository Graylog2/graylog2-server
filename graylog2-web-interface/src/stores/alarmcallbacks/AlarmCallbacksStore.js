import Reflux from 'reflux';

import ActionsProvider from 'injection/ActionsProvider';
const AlarmCallbacksActions = ActionsProvider.getActions('AlarmCallbacks');

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const AlarmCallbacksStore = Reflux.createStore({
  listenables: [AlarmCallbacksActions],

  available(streamId) {
    const failCallback = (error) =>
      UserNotification.error(`Fetching available AlarmCallback types failed with status: ${error.message}`,
        'Could not retrieve available AlarmCallbacks');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.available(streamId).url);
    const promise = fetch('GET', url).then((response) => {
      this.trigger({ availableAlarmCallbacks: response.types });
      return response.types;
    }, failCallback);

    AlarmCallbacksActions.available.promise(promise);

    return promise;
  },
  list(streamId) {
    const failCallback = (error) =>
      UserNotification.error(`Fetching AlarmCallbacks failed with status: ${error.message}`,
        'Could not retrieve AlarmCallbacks');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.list(streamId).url);
    const promise = fetch('GET', url).then((response) => response.alarmcallbacks, failCallback);

    AlarmCallbacksActions.list.promise(promise);
  },
  save(streamId, alarmCallback) {
    const failCallback = (error) =>
      UserNotification.error(`Saving AlarmCallback failed with status: ${error.message}`,
        'Could not save AlarmCallback');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.create(streamId).url);

    const promise = fetch('POST', url, alarmCallback).catch(failCallback);

    AlarmCallbacksActions.save.promise(promise);
  },
  delete(streamId, alarmCallbackId) {
    const failCallback = (error) =>
      UserNotification.error(`Removing AlarmCallback failed with status: ${error.message}`,
        'Could not remove AlarmCallback');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.delete(streamId, alarmCallbackId).url);

    const promise = fetch('DELETE', url).catch(failCallback);

    AlarmCallbacksActions.delete.promise(promise);
  },
  update(streamId, alarmCallbackId, deltas) {
    const failCallback = (error) =>
      UserNotification.error(`Updating Alarm Callback '${alarmCallbackId}' failed with status: ${error.message}`,
        'Could not update Alarm Callback');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.update(streamId, alarmCallbackId).url);

    const promise = fetch('PUT', url, deltas).catch(failCallback);

    AlarmCallbacksActions.update.promise(promise);
  },
});

export default AlarmCallbacksStore;
