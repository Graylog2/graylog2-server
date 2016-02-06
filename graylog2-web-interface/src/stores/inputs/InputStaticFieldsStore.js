import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

const InputStaticFieldsStore = Reflux.createStore({
  listenables: [],
  sourceUrl: (inputId) => `/system/inputs/${inputId}/staticfields`,

  create(input, name, value) {
    const url = URLUtils.qualifyUrl(this.sourceUrl(input.id));
    const promise = fetch('POST', url, {key: name, value: value});
    promise
      .then(() => {
        this.trigger({});
        UserNotification.success(`Static field '${name}' added to '${input.title}' successfully`);
      })
      .catch(error => {
        UserNotification.error(`Adding static field to input failed with: ${error}`,
          `Could not add static field to input '${input.title}'`);
      });

    return promise;
  },

  destroy(input, name) {
    const url = URLUtils.qualifyUrl(`${this.sourceUrl(input.id)}/${name}`);
    const promise = fetch('DELETE', url);
    promise
      .then(() => {
        this.trigger({});
        UserNotification.success(`Static field '${name}' removed from '${input.title}' successfully`);
      })
      .catch(error => {
        UserNotification.error(`Removing static field from input failed with: ${error}`,
          `Could not remove static field '${name} from input '${input.title}'`);
      });

    return promise;
  },
});

export default InputStaticFieldsStore;
