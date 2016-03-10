import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import AlertsActions from 'actions/alerts/AlertsActions';

const AlertsStore = Reflux.createStore({
  listenables: [AlertsActions],

  list(streamId, skip, limit) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.list(streamId, skip, limit).url);
    const promise = fetch('GET', url);
    promise
      .then(
        response => this.trigger({ alerts: response }),
        error => {
          UserNotification.error(`Fetching alerts failed with status: ${error.message}`, 'Could not retrieve alerts.');
        });

    AlertsActions.list.promise(promise);
  },

  listAllStreams(since) {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertsApiController.listAllStreams(since).url);
    const promise = fetch('GET', url);
    promise
      .then(
        response => this.trigger({ alerts: response }),
        error => {
          UserNotification.error(`Fetching alerts failed with status: ${error.message}`, 'Could not retrieve alerts.');
        });

    AlertsActions.listAllStreams.promise(promise);
  },
});

export default AlertsStore;
