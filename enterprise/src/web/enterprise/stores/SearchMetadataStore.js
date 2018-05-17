import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';
import Immutable from 'immutable';

const parseSearchUrl = URLUtils.qualifyUrl('/plugins/org.graylog.plugins.enterprise/search/metadata');
const parseSearchIdUrl = id => URLUtils.qualifyUrl(`/plugins/org.graylog.plugins.enterprise/search/metadata/${id}`);

const getUndeclaredParameters = metadata => metadata.getIn(['parameters', 'undeclared'], Immutable.Set());
const getUsedParameters = metadata => metadata.getIn(['parameters', 'used'], Immutable.Set());

export { getUndeclaredParameters, getUsedParameters };

export const SearchMetadataActions = Reflux.createActions({
  parseSearch: { asyncResult: true },
  parseSearchId: { asyncResult: true },
});

export const SearchMetadataStore = Reflux.createStore({
  listenables: [SearchMetadataActions],

  state: Immutable.fromJS({
    parameters: {
      undeclared: Immutable.Set(),
      used: Immutable.Set(),
    },
  }),

  getInitialState() {
    return this.state;
  },

  _postProcess(metadata) {
    // turn the server response in something the UI regularly needs:
    // the server returns parameters for each query in the search request, the ui primarily needs a global view onto parameters to display them
    const queryMD = Immutable.fromJS(metadata.query_metadata);
    const declaredParams = Immutable.fromJS(metadata.declared_parameters);
    let undeclared = Immutable.Set();
    let used = Immutable.Set();
    queryMD.forEach((params) => {
      params.get('used_parameters_names').forEach((parameter) => {
        if (declaredParams.has(parameter)) {
          used = used.add(declaredParams.get(parameter));
        } else {
          undeclared = undeclared.add(parameter);
        }
      });
    });
    return Immutable.Map({
      undeclared: undeclared,
      used: used,
    });
  },

  parseSearch(searchRequest) {
    const promise = fetch('POST', parseSearchUrl, JSON.stringify(searchRequest))
      .then((metadata) => {
        this.state = this.state.set('parameters', this._postProcess(metadata));
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearch.promise(promise);
  },

  parseSearchId(searchId) {
    const promise = fetch('GET', parseSearchIdUrl, searchId)
      .then((metadata) => {
        this.state = this.state.set('parameters', this._postProcess(metadata));
        this._trigger();
        return this.state;
      });
    SearchMetadataActions.parseSearchId.promise(promise);
  },

  _trigger() {
    this.trigger(this.state);
  },
});
