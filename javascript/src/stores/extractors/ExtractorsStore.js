import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';

const ExtractorsStore = Reflux.createStore({
  listenables: [ExtractorsActions],
  sourceUrl: '/system/inputs/',
  extractors: undefined,
  extractor: undefined,

  init() {
    this.trigger({extractors: this.extractors, extractor: this.extractor});
  },

  list(inputId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors')))
      .then(response => {
        this.extractors = response.extractors;
        this.trigger({extractors: this.extractors});
      });

    ExtractorsActions.list.promise(promise);
  },

  get(inputId, extractorId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors', extractorId)))
      .then(response => {
        this.extractor = response;
        this.trigger({extractor: this.extractor});
      });

    ExtractorsActions.get.promise(promise);
  },
});

export default ExtractorsStore;
