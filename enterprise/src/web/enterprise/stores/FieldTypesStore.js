import Reflux from 'reflux';
import Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';

import FieldTypesActions from '../actions/FieldTypesActions';

const fieldTypesUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/fields');

export default Reflux.createStore({
  listenables: [FieldTypesActions],

  fields: undefined,

  init() {
    this.all();
  },

  getInitialState() {
    return this.fields;
  },

  all() {
    const promise = fetch('GET', fieldTypesUrl).then((response) => {
      this.fields = Immutable.fromJS(response);
      this.trigger(this.fields);
    });

    FieldTypesActions.all.promise(promise);

    return promise;
  },

  forStreams(streams) {
    const promise = fetch('POST', fieldTypesUrl, { streams: streams });
    FieldTypesActions.forStreams.promise(promise);
    return promise;
  },
});
