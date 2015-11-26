import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

import InputsActions from 'actions/inputs/InputsActions';

const InputsStore = Reflux.createStore({
  listenables: [InputsActions],
  sourceUrl: '/system/inputs',
  inputs: undefined,
  input: undefined,

  init() {
    this.trigger({inputs: this.inputs, input: this.input});
  },

  list(completeInput) {
    const promise = fetch('GET', URLUtils.qualifyUrl(this.sourceUrl))
      .then(response => {
        this.inputs = (completeInput ? response.inputs : response.inputs.map((input) => input.message_input));
        this.trigger({inputs: this.inputs});

        return this.inputs;
      })
      .catch(error => {
        UserNotification.error('Fetching Inputs failed with status: ' + error,
          'Could not retrieve Inputs');
      });

    InputsActions.list.promise(promise);
  },

  get(inputId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(`${this.sourceUrl}/${inputId}`));

    promise
      .then(response => {
        this.input = response;
        this.trigger({input: this.input});

        return this.input;
      })
      .catch(errorThrown => {
        UserNotification.error(`Fetching input ${inputId} failed with status: ${errorThrown}`,
          'Could not retrieve input');
      });

    InputsActions.get.promise(promise);
  },
});

InputsStore.inputsAsMap = (inputsList) => {
  const inputsMap = {};
  inputsList.forEach(input => {
    inputsMap[input.input_id] = input;
  });
  return inputsMap;
};

export default InputsStore;
