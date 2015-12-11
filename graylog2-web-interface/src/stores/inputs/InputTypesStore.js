import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import InputTypesActions from 'actions/inputs/InputTypesActions';

const InputTypesStore = Reflux.createStore({
  listenables: [InputTypesActions],
  sourceUrl: '/system/inputs/types',
  inputTypes: undefined,

  init() {
    this.list();
  },

  getInitialState() {
    return {inputTypes: this.inputTypes};
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl));
    promise
      .then(response => {
        this.inputTypes = response.types;
        this.trigger({inputTypes: this.inputTypes});
      })
      .catch(error => {
        UserNotification.error('Fetching Input Types failed with status: ' + error,
          'Could not retrieve Inputs');
      });

    InputTypesActions.list.promise(promise);
  },

  get(inputTypeId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${inputTypeId}`));

    promise
      .catch(error => {
        UserNotification.error(`Fetching input ${inputTypeId} failed with status: ${error}`,
          'Could not retrieve input');
      });

    InputTypesActions.get.promise(promise);
  },
});

export default InputTypesStore;
