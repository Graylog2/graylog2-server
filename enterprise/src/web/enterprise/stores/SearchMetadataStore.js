import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import Immutable from 'immutable';

import SearchMetadataActions from '../actions/SearchMetadataActions';

const parseSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search/metadata');
const parseSearchIdUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/metadata/${id}`);

export default Reflux.createStore({
  listenables: [SearchMetadataActions],

  state: {
    parameters: {},
  },

  getInitialState() {
    return {
      state: this.state,
    };
  },

  _postProcess(metadata) {
    // turn the server response in something the UI regularly needs:
    // the server returns parameters for each query in the search request, the ui primarly needs a global view onto parameters to display them
    const queryMD = Immutable.fromJS(metadata.query_metadata);
    return Immutable.Map().withMutations((map) => {
      queryMD.forEach((params) => {
        params.get('parameters').forEach((parameter) => {
          /*
           parameter = {
          "name": "DEST_IP",
          "optional": false,
          "binding": {
            "type": "query_ref",
            "query_id": "e6dd17e4-9c33-4327-8fbf-85cd7b353253",
            "search_type_id": "5e7fa779-221f-4150-a33a-68e3fc2786a6",
            "value": "$.groups[0].fields[0].value",
            "type": "query_ref"
          },
          "data_type": "ipaddress",
          "default": "192.168.1.1"
        }
           */
          map.set(parameter.get('name'), parameter);
        });
        params.get('unused_parameters').forEach((parameter) => {
          map.set(parameter.get('name'), parameter);
        });
      });
    }).toJS();
  },

  parseSearch(searchRequest) {
    const promise = fetch('POST', parseSearchUrl, searchRequest.toRequest())
      .then((metadata) => {
        this.state.parameters = this._postProcess(metadata);
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearch.promise(promise);
  },

  parseSearchId(searchId) {
    const promise = fetch('GET', parseSearchIdUrl, searchId)
      .then((metadata) => {
        this.state.parameters = this._postProcess(metadata);
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearchId.promise(promise);
  },

  _trigger() {
    this.trigger(this.state);
  },
});
