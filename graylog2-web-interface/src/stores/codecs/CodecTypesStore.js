import Reflux from 'reflux';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import CodecTypesActions from 'actions/codecs/CodecTypesActions';

const CodecTypesStore = Reflux.createStore({
  listenables: [CodecTypesActions],
  codecTypes: undefined,

  getInitialState() {
    return { codecTypes: this.codecTypes };
  },

  list() {
    const promise = fetch('GET', URLUtils.qualifyUrl(ApiRoutes.CodecTypesController.list().url));
    promise.then(
        response => {
          this.codecTypes = response;
          this.trigger(this.getInitialState());
        },
        error => {
          UserNotification.error(`Fetching codec types failed with status: ${error}`,
            'Could not retrieve codec types');
        });

    CodecTypesActions.list.promise(promise);
  },
});

export default CodecTypesStore;
