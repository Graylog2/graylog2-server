import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';
const InputTypesActions = ActionsProvider.getActions('InputTypes');

const InputTypesStore = Reflux.createStore({
  listenables: [InputTypesActions],
  sourceUrl: '/system/inputs/types',
  inputTypes: undefined,
  inputDescriptions: undefined,

  init() {
    this.list();
  },

  getInitialState() {
    return { inputTypes: this.inputTypes, inputDescriptions: this.inputDescriptions };
  },

  list() {
    const promiseTypes = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    const promiseDescriptions = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/all`));
    const promise = Promise.all([promiseTypes, promiseDescriptions]);
    promise
      .then(
        (responses) => {
          this.inputTypes = responses[0].types;
          this.inputDescriptions = responses[1];
          this.trigger(this.getInitialState());
        },
        (error) => {
          UserNotification.error(`Fetching Input Types failed with status: ${error}`,
            'Could not retrieve Inputs');
        });

    InputTypesActions.list.promise(promise);
  },

  get(inputTypeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${inputTypeId}`));

    promise
      .catch((error) => {
        UserNotification.error(`Fetching input ${inputTypeId} failed with status: ${error}`,
          'Could not retrieve input');
      });

    InputTypesActions.get.promise(promise);
  },
});

export default InputTypesStore;
