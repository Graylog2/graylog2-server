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
import merge from 'lodash/merge';

import type { Store } from 'stores/StoreTypes';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

type ConfigurationVariableActionsType = {
  all: () => Promise<unknown>;
  save: (configurationVariable: { id: string; name: string; description: string; content: string }) => Promise<unknown>;
  delete: (configurationVariable: { id: string; name: string }) => Promise<unknown>;
  validate: (configurationVariable: { name?: string; [key: string]: unknown }) => Promise<unknown>;
  getConfigurations: (configurationVariable: { id: string }) => Promise<unknown>;
};

export const ConfigurationVariableActions = singletonActions('core.ConfigurationVariable', () =>
  Reflux.createActions<ConfigurationVariableActionsType>({
    all: { asyncResult: true },
    save: { asyncResult: true },
    delete: { asyncResult: true },
    validate: { asyncResult: true },
    getConfigurations: { asyncResult: true },
  }),
);

export const ConfigurationVariableStore: Store<{}> = singletonStore('core.ConfigurationVariable', () =>
  Reflux.createStore({
    listenables: [ConfigurationVariableActions],
    sourceUrl: '/sidecar/configuration_variables',

    all() {
      const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));

      promise.catch((error: unknown) => {
        UserNotification.error(
          `Fetching configuration variables failed with status: ${error}`,
          'Could not retrieve configuration variables',
        );
      });

      ConfigurationVariableActions.all.promise(promise);
    },

    save(configurationVariable: { id: string; name: string; description: string; content: string }) {
      const request = {
        id: configurationVariable.id,
        name: configurationVariable.name,
        description: configurationVariable.description,
        content: configurationVariable.content,
      };

      let url = URLUtils.qualifyUrl(`${this.sourceUrl}`);
      let method: 'POST' | 'PUT';
      let action: string;

      if (configurationVariable.id === '') {
        method = 'POST';
        action = 'created';
      } else {
        url += `/${configurationVariable.id}`;
        method = 'PUT';
        action = 'updated';
      }

      const promise = fetch(method, url, request);

      promise.then(
        () => {
          UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully ${action}`);
        },
        (error: { message?: string }) => {
          UserNotification.error(
            `Saving variable "${configurationVariable.name}" failed with status: ${error.message}`,
            'Could not save variable',
          );
        },
      );

      ConfigurationVariableActions.save.promise(promise);
    },

    getConfigurations(configurationVariable: { id: string }) {
      const url = URLUtils.qualifyUrl(`${this.sourceUrl}/${configurationVariable.id}/configurations`);
      const promise = fetch('GET', url);

      promise.catch((error: unknown) => {
        UserNotification.error(`Fetching configurations for this variable failed with status: ${error}`);
      });

      ConfigurationVariableActions.getConfigurations.promise(promise);
    },

    delete(configurationVariable: { id: string; name: string }) {
      const url = URLUtils.qualifyUrl(`${this.sourceUrl}/${configurationVariable.id}`);
      const promise = fetch('DELETE', url);

      promise.then(
        () => {
          UserNotification.success(`Configuration variable "${configurationVariable.name}" successfully deleted`);
        },
        (error: { message?: string }) => {
          UserNotification.error(
            `Deleting variable "${configurationVariable.name}" failed with status: ${error.message}`,
            'Could not delete variable',
          );
        },
      );

      ConfigurationVariableActions.delete.promise(promise);
    },

    validate(configurationVariable: { name?: string; [key: string]: unknown }) {
      // set minimum api defaults for faster validation feedback
      const payload: Record<string, unknown> = {
        id: ' ',
        name: ' ',
        content: ' ',
      };

      merge(payload, configurationVariable);

      const promise = fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl}/validate`), payload);

      promise.catch((error: { message?: string }) => {
        UserNotification.error(
          `Validating variable "${configurationVariable.name}" failed with status: ${error.message}`,
          'Could not validate variable',
        );
      });

      ConfigurationVariableActions.validate.promise(promise);
    },
  }),
);
