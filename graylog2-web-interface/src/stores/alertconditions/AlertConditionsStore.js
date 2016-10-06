import Reflux from 'reflux';
import _ from 'lodash';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const AlertConditionsActions = ActionsProvider.getActions('AlertConditions');

const AlertConditionsStore = Reflux.createStore({
  listenables: AlertConditionsActions,

  init() {
    this.available();
  },

  getInitialState() {
    return {
      types: this.types,
    };
  },

  available() {
    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.available().url);
    const promise = fetch('GET', url).then((response) => {
      this.types = response;
      this.trigger(this.getInitialState());
    });

    AlertConditionsActions.available.promise(promise);
    return promise;
  },

  delete(streamId, alertConditionId) {
    const failCallback = (error) => {
      UserNotification.error(`Removing Alert Condition failed with status: ${error}`,
        'Could not remove Alert Conditions');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.delete(streamId, alertConditionId).url);
    const promise = fetch('DELETE', url).then(() => {
      AlertConditionsActions.list(streamId);
    }, failCallback);
    AlertConditionsActions.delete.promise(promise);
    return promise;
  },
  list(streamId) {
    const failCallback = (error) => {
      UserNotification.error(`Fetching Alert Conditions failed with status: ${error}`,
        'Could not retrieve Alert Conditions');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.list(streamId).url);
    const promise = fetch('GET', url).then((response) => {
      const conditions = response.conditions.map((condition) => {
        const cond = _.clone(condition);
        cond.stream_id = streamId;
        return cond;
      });
      this.trigger({ alertConditions: conditions });
      return conditions;
    }, failCallback);

    AlertConditionsActions.list.promise(promise);
    return promise;
  },
  save(streamId, alertCondition) {
    const failCallback = (error) => {
      UserNotification.error(`Saving Alert Condition failed with status: ${error}`,
        'Could not save Alert Condition');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.create(streamId).url);
    const promise = fetch('POST', url, alertCondition).then(() => {
      AlertConditionsActions.list(streamId);
    }, failCallback);

    AlertConditionsActions.save.promise(promise);
    return promise;
  },
  update(streamId, alertConditionId, request) {
    const failCallback = (error) => {
      UserNotification.error(`Saving Alert Condition failed with status: ${error}`,
        'Could not save Alert Condition');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.update(streamId, alertConditionId).url);
    const promise = fetch('PUT', url, request).then(() => {
      AlertConditionsActions.list(streamId);
    }, failCallback);

    AlertConditionsActions.update.promise(promise);
    return promise;
  },
});

export default AlertConditionsStore;
