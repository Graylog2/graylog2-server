import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import StoreProvider from 'injection/StoreProvider';
const InputStaticFieldsStore = StoreProvider.getStore('InputStaticFields');

import ActionsProvider from 'injection/ActionsProvider';
const InputsActions = ActionsProvider.getActions('Inputs');

const InputsStore = Reflux.createStore({
  listenables: [InputsActions],
  sourceUrl: '/system/inputs',
  inputs: undefined,
  input: undefined,

  init() {
    this.trigger({ inputs: this.inputs, input: this.input });
    this.listenTo(InputStaticFieldsStore, this.list);
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    promise
      .then(
        (response) => {
          this.inputs = response.inputs;
          this.trigger({ inputs: this.inputs });

          return this.inputs;
        },
        (error) => {
          UserNotification.error(`Fetching Inputs failed with status: ${error}`,
            'Could not retrieve Inputs');
        });

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
          this.trigger({ input: this.input });

          return this.input;
        },
        (error) => {
          if (showError) {
            UserNotification.error(`Fetching input ${inputId} failed with status: ${error}`,
                                   'Could not retrieve input');
          } else {
            this.trigger({ input: {} });
          }
        });

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
        });

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
        });

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
        });

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
