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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import ActionsProvider from 'injection/ActionsProvider';

const InputTypesActions = ActionsProvider.getActions('InputTypes');

export type InputTypes = {
  [type: string]: string,
};

export type ConfigurationField = {
  field_type: 'boolean' | 'text' | 'dropdown' | 'list' | 'number',
  name: string,
  human_name: string,
  description: string,
  default_value: any,
  is_optional: boolean,
  attributes: Array<string>,
  additional_info: any,
  position: number,
};

export type InputDescription = {
  type: string,
  name: string,
  is_exclusive: boolean,
  requested_configuration: {
    [key: string]: ConfigurationField,
  },
  link_to_docs: string,
};

export type InputDescriptions = {
  [type: string]: InputDescription,
};

const InputTypesStore = Reflux.createStore({
  listenables: [InputTypesActions],
  sourceUrl: '/system/inputs/types',
  inputTypes: undefined,
  inputDescriptions: undefined,

  init() {
    this.list();
  },

  getInitialState(): { inputTypes: InputTypes, inputDescriptions: InputDescriptions } {
    return { inputTypes: this.inputTypes, inputDescriptions: this.inputDescriptions };
  },

  list() {
    const promiseTypes = fetch('GET', qualifyUrl(this.sourceUrl));
    const promiseDescriptions = fetch('GET', qualifyUrl(`${this.sourceUrl}/all`));
    const promise: Promise<[{ types: InputTypes }, InputDescriptions]> = Promise.all([promiseTypes, promiseDescriptions]);

    promise
      .then(
        ([typesResponse, descriptionsResponse]) => {
          this.inputTypes = typesResponse.types;
          this.inputDescriptions = descriptionsResponse;
          this.trigger(this.getInitialState());
        },
        (error) => {
          UserNotification.error(`Fetching Input Types failed with status: ${error}`,
            'Could not retrieve Inputs');
        },
      );

    InputTypesActions.list.promise(promise);
  },

  get(inputTypeId: string) {
    const promise = fetch('GET', qualifyUrl(`${this.sourceUrl}/${inputTypeId}`));

    promise
      .catch((error) => {
        UserNotification.error(`Fetching input ${inputTypeId} failed with status: ${error}`,
          'Could not retrieve input');
      });

    InputTypesActions.get.promise(promise);
  },
});

export default InputTypesStore;
