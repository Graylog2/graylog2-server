// @flow strict
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
    const promise = Promise.all([promiseTypes, promiseDescriptions]);
    promise
      .then(
        ([typesResponse: { types: InputTypes }, descriptionsResponse: InputDescriptions]) => {
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
