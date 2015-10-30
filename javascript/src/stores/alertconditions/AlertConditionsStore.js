import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import AlertConditionsActions from 'actions/alertconditions/AlertConditionsActions';

const AlertConditionsStore = Reflux.createStore({
  listenables: AlertConditionsActions,

  delete(streamId, alertConditionId) {
    const failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error('Removing Alert Condition failed with status: ' + errorThrown,
        'Could not remove Alert Conditions');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.StreamAlertsApiController.delete(streamId, alertConditionId).url);
    const promise = fetch('DELETE', url).then(() => {
      AlertConditionsActions.list(streamId);
    });
    AlertConditionsActions.delete.promise(promise);
  },
  list(streamId) {
    const failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error('Fetching Alert Conditions failed with status: ' + errorThrown,
        'Could not retrieve Alert Conditions');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.StreamAlertsApiController.list(streamId).url);
    const promise = fetch('GET', url).then((response) => {
      const conditions = response.conditions.map((condition => {
        condition.stream_id = streamId;
        return condition;
      }));
      this.trigger({alertConditions: conditions});
      return conditions;
    }, failCallback);

    AlertConditionsActions.list.promise(promise);
    return promise;
  },
  save(streamId, alertCondition) {
    const failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error('Saving Alert Condition failed with status: ' + errorThrown,
        'Could not save Alert Condition');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.StreamAlertsApiController.create(streamId).url);
    const promise = fetch('POST', url, alertCondition).then(() => {
      AlertConditionsActions.list(streamId);
    }, failCallback);

    AlertConditionsActions.save.promise(promise);
  },
  update(streamId, alertConditionId, request) {
    const failCallback = (jqXHR, textStatus, errorThrown) => {
      UserNotification.error('Saving Alert Condition failed with status: ' + errorThrown,
        'Could not save Alert Condition');
    };

    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.StreamAlertsApiController.update(streamId, alertConditionId).url);
    const promise = fetch('PUT', url, request).then(() => {
      AlertConditionsActions.list(streamId);
    }, failCallback);

    AlertConditionsActions.update.promise(promise);
  },
});

export default AlertConditionsStore;
