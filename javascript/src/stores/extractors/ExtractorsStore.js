import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';

const ExtractorsStore = Reflux.createStore({
  listenables: [ExtractorsActions],
  sourceUrl: '/system/inputs/',
  extractors: undefined,

  init() {
    this._propagateState();
  },

  getInitialState() {
    return this.getExtractorsInfo();
  },

  getExtractorsInfo() {
    return {extractors: this.extractors};
  },

  _propagateState() {
    this.trigger(this.getExtractorsInfo());
  },

  list(inputId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors')))
      .then(response => {
        this.extractors = response.extractors;
        this._propagateState();
      });

    ExtractorsActions.list.promise(promise);
  },
});

export default ExtractorsStore;
