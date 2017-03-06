import Reflux from 'reflux';

import ActionsProvider from 'injection/ActionsProvider';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const AlarmCallbacksActions = ActionsProvider.getActions('AlarmCallbacks');

const AlarmCallbacksStore = Reflux.createStore({
  listenables: [AlarmCallbacksActions],

  list(streamId) {
    const failCallback = error =>
      UserNotification.error(`Fetching alert notifications failed with status: ${error.message}`,
        'Could not retrieve alert notification');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.list(streamId).url);
    const promise = fetch('GET', url).then(response => response.alarmcallbacks, failCallback);

    AlarmCallbacksActions.list.promise(promise);
  },
  save(streamId, alarmCallback) {
    const failCallback = (error) => {
      const errorMessage = (error.additional && error.additional.status === 400 ? error.additional.body.message : error.message);
      UserNotification.error(`Saving alert notification failed with status: ${errorMessage}`,
        'Could not save alert notification');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.create(streamId).url);

    const promise = fetch('POST', url, alarmCallback);
    promise.then(
      () => UserNotification.success('Alert notification saved successfully'),
      failCallback,
    );

    AlarmCallbacksActions.save.promise(promise);
  },
  delete(streamId, alarmCallbackId) {
    const failCallback = error =>
      UserNotification.error(`Removing alert notification failed with status: ${error.message}`,
        'Could not remove alert notification');

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.delete(streamId, alarmCallbackId).url);

    const promise = fetch('DELETE', url);
    promise.then(
      () => UserNotification.success('Alert notification deleted successfully'),
      failCallback,
    );

    AlarmCallbacksActions.delete.promise(promise);
  },
  update(streamId, alarmCallbackId, deltas) {
    const failCallback = (error) => {
      const errorMessage = (error.additional && error.additional.status === 400 ? error.additional.body.message : error.message);
      UserNotification.error(`Updating alert notification '${alarmCallbackId}' failed with status: ${errorMessage}`,
        'Could not update alert notification');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.AlarmCallbacksApiController.update(streamId, alarmCallbackId).url);

    const promise = fetch('PUT', url, deltas);
    promise.then(
      () => UserNotification.success('Alert notification updated successfully'),
      failCallback,
    );

    AlarmCallbacksActions.update.promise(promise);
  },
});

export default AlarmCallbacksStore;
