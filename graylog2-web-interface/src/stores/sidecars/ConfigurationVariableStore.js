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
import lodash from 'lodash';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

const ConfigurationVariableStore = Reflux.createStore({
  listenables: [ConfigurationVariableActions],
  sourceUrl: '/sidecar/configuration_variables',

  all() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));

    promise
      .catch(
        (error) => {
          UserNotification.error(`Fetching configuration variables failed with status: ${error}`,
            'Could not retrieve configuration variables');
        },
      );

    ConfigurationVariableActions.all.promise(promise);
  },

  save(configurationVariable) {
    const request = {
      id: configurationVariable.id,
      name: configurationVariable.name,
      description: configurationVariable.description,
      content: configurationVariable.content,
    };

    let url = URLUtils.qualifyUrl(`${this.sourceUrl}`);
    let method;
    let action;

    if (configurationVariable.id === '') {
      method = 'POST';
      action = 'created';
    } else {
      url += `/${configurationVariable.id}`;
      method = 'PUT';
      action = 'updated';
    }

    const promise = fetch(method, url, request);

    promise
      .then(() => {
        UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully ${action}`);
      }, (error) => {
        UserNotification.error(`Saving variable "${configurationVariable.name}" failed with status: ${error.message}`,
          'Could not save variable');
      });

    ConfigurationVariableActions.save.promise(promise);
  },

  getConfigurations(configurationVariable) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/${configurationVariable.id}/configurations`);
    const promise = fetch('GET', url);

    promise.catch(
      (error) => {
        UserNotification.error(`Fetching configurations for this variable failed with status: ${error}`);
      },
    );

    ConfigurationVariableActions.getConfigurations.promise(promise);
  },

  delete(configurationVariable) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl}/${configurationVariable.id}`);
    const promise = fetch('DELETE', url);

    promise
      .then(() => {
        UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully deleted`);
      }, (error) => {
        UserNotification.error(`Deleting variable "${configurationVariable.name}" failed with status: ${error.message}`,
          'Could not delete variable');
      });

    ConfigurationVariableActions.delete.promise(promise);
  },

  validate(configurationVariable) {
    // set minimum api defaults for faster validation feedback
    const payload = {
      id: ' ',
      name: ' ',
      content: ' ',
    };

    lodash.merge(payload, configurationVariable);

    const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/validate`), payload);

    promise.catch(
      (error) => {
        UserNotification.error(`Validating variable "${configurationVariable.name}" failed with status: ${error.message}`,
          'Could not validate variable');
      },
    );

    ConfigurationVariableActions.validate.promise(promise);
  },

});

export default ConfigurationVariableStore;
