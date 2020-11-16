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

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const InputStaticFieldsStore = StoreProvider.getStore('InputStaticFields');
const InputsActions = ActionsProvider.getActions('Inputs');

const InputsStore = Reflux.createStore({
  listenables: [InputsActions],
  sourceUrl: '/system/inputs',
  inputs: undefined,
  input: undefined,

  init() {
    this.trigger(this._state());
    this.listenTo(InputStaticFieldsStore, this.list);
  },

  getInitialState() {
    return this._state();
  },

  _state() {
    return { inputs: this.inputs, input: this.input };
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));

    promise
      .then(
        (response) => {
          this.inputs = response.inputs;
          this.trigger(this._state());

          return this.inputs;
        },
        (error) => {
          UserNotification.error(`Fetching Inputs failed with status: ${error}`,
            'Could not retrieve Inputs');
        },
      );

    InputsActions.list.promise(promise);
  },

  get(inputId) {
    return this.getOptional(inputId, true);
  },

  getOptional(inputId, showError) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${inputId}`));

    promise
      .then(
        (response) => {
          this.input = response;
          this.trigger(this._state());

          return this.input;
        },
        (error) => {
          if (showError) {
            UserNotification.error(`Fetching input ${inputId} failed with status: ${error}`,
              'Could not retrieve input');
          } else {
            this.trigger(this._state());
          }
        },
      );

    InputsActions.get.promise(promise);
  },

  create(input) {
    const promise = fetch('POST', URLUtils.qualifyUrl(this.sourceUrl), input);

    promise
      .then(
        () => {
          UserNotification.success(`Input '${input.title}' launched successfully`);
          InputsActions.list();
        },
        (error) => {
          UserNotification.error(`Launching input '${input.title}' failed with status: ${error}`,
            'Could not launch input');
        },
      );

    InputsActions.create.promise(promise);
  },

  delete(input) {
    const inputId = input.id;
    const inputTitle = input.title;

    const promise = fetch('DELETE', URLUtils.qualifyUrl(`${this.sourceUrl}/${inputId}`));

    promise
      .then(
        () => {
          UserNotification.success(`Input '${inputTitle}' deleted successfully`);
          InputsActions.list();
        },
        (error) => {
          UserNotification.error(`Deleting input '${inputTitle}' failed with status: ${error}`,
            'Could not delete input');
        },
      );

    InputsActions.delete.promise(promise);
  },

  update(id, input) {
    const promise = fetch('PUT', URLUtils.qualifyUrl(`${this.sourceUrl}/${id}`), input);

    promise
      .then(
        () => {
          UserNotification.success(`Input '${input.title}' updated successfully`);
          InputsActions.list();
        },
        (error) => {
          UserNotification.error(`Updating input '${input.title}' failed with status: ${error}`,
            'Could not update input');
        },
      );

    InputsActions.update.promise(promise);
  },
});

InputsStore.inputsAsMap = (inputsList) => {
  const inputsMap = {};

  inputsList.forEach((input) => {
    inputsMap[input.id] = input;
  });

  return inputsMap;
};

export default InputsStore;
