import Reflux from 'reflux';
import md5 from 'md5';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

const MessageFieldsStore = Reflux.createStore({
  listenables: [],
  fields: undefined,

  init() {
    this.list();
  },
  getInitialState() {
    return {fields: this.fields};
  },
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.MessageFieldsApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      const result = response.fields.map((field) => {
        return {
          hash: md5(field),
          name: field,
          standard_selected: (field === 'message' || field === 'source'),
        };
      });
      this.fields = result;
      this.trigger(this.getInitialState());
      return result;
    });
    return promise;
  },
});

export default MessageFieldsStore;
