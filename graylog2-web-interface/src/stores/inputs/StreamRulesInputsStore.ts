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

import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';
import type { Input } from 'components/messageloaders/Types';

export type PaginatedStreamRulesInputs = {
  list: Array<Input>,
};

type StreamRulesInputsActionsType = {
  list: () => Promise<{ inputs: Array<Input>, total: number }>,
}

type StreamRulesInputsStoreState = {
  inputs: Array<Input> | undefined,
}

export const StreamRulesInputsActions = singletonActions(
  'core.StreamRulesInputs',
  () => Reflux.createActions<StreamRulesInputsActionsType>({
    list: { asyncResult: true },
  }),
);

export const StreamRulesInputsStore = singletonStore(
  'core.StreamRulesInputs',
  () => Reflux.createStore<StreamRulesInputsStoreState>({
    listenables: [StreamRulesInputsActions],
    sourceUrl: '/streams/rules/inputs',
    inputs: undefined,

    init() {
      this.list();
    },

    _state() {
      return { inputs: this.inputs };
    },

    getInitialState() {
      return this._state();
    },

    list() {
      const promise = fetch('GET', qualifyUrl(this.sourceUrl));

      promise
        .then(
          (response) => {
            this.inputs = response.inputs;
            this.trigger(this._state());

            return this.inputs;
          },
          (error) => {
            UserNotification.error(`Fetching Stream Rule Inputs List failed with status: ${error}`,
              'Could not retrieve Stream Rule Inputs');
          },
        );

      StreamRulesInputsActions.list.promise(promise);
    },
  }),
);
