/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';
import _ from 'lodash';

import UserNotification from 'util/UserNotification';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const AlertConditionsActions = ActionsProvider.getActions('AlertConditions');

const AlertConditionsStore = Reflux.createStore({
  listenables: AlertConditionsActions,
  allAlertConditions: undefined,
  availableConditions: undefined,

  getInitialState() {
    return {
      availableConditions: this.availableConditions,
      allAlertConditions: this.allAlertConditions,
    };
  },

  available() {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertConditionsApiController.available().url);
    const promise = fetch('GET', url).then((response) => {
      this.availableConditions = response;
      this.trigger({ availableConditions: this.availableConditions });
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
      AlertConditionsActions.listAll();
      UserNotification.success('Condition deleted successfully');
    }, failCallback);

    AlertConditionsActions.delete.promise(promise);

    return promise;
  },

  listAll() {
    const url = URLUtils.qualifyUrl(ApiRoutes.AlertConditionsApiController.list().url);
    const promise = fetch('GET', url).then(
      (response) => {
        this.allAlertConditions = response.conditions;
        this.trigger({ allAlertConditions: this.allAlertConditions });

        return this.allAlertConditions;
      },
      (error) => {
        UserNotification.error(`Fetching alert conditions failed with status: ${error}`,
          'Could not get alert conditions');
      },
    );

    AlertConditionsActions.listAll.promise(promise);
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
    const promise = fetch('POST', url, alertCondition).then((response) => {
      UserNotification.success('Condition created successfully');

      return response.alert_condition_id;
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
    const promise = fetch('PUT', url, request).then((response) => {
      UserNotification.success('Condition updated successfully');

      return response;
    }, failCallback);

    AlertConditionsActions.update.promise(promise);

    return promise;
  },
  get(streamId, conditionId, failureCallback) {
    const failCallback = (error) => {
      UserNotification.error(`Fetching Alert Condition ${conditionId} failed with status: ${error}`,
        'Could not retrieve Alert Condition');
    };

    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.get(streamId, conditionId).url);
    const promise = fetch('GET', url);

    promise.then(
      (response) => {
        this.trigger({ alertCondition: response });

        return response;
      },
      (error) => {
        return (typeof failureCallback === 'function' ? failureCallback(error) : failCallback(error));
      },
    );

    AlertConditionsActions.get.promise(promise);
  },

  test(streamId, conditionId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.StreamAlertsApiController.test(streamId, conditionId).url);
    const promise = fetch('POST', url);

    AlertConditionsActions.test.promise(promise);
  },
});

export default AlertConditionsStore;
