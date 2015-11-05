import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';

const ExtractorsStore = Reflux.createStore({
  listenables: [ExtractorsActions],
  sourceUrl: '/system/inputs/',

  list(inputId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors')))
      .then(response => {
        this.trigger({extractors: response.extractors});
      });

    ExtractorsActions.list.promise(promise);
  },

  get(inputId, extractorId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors', extractorId)))
      .then(response => {
        this.trigger({extractor: response});
      });

    ExtractorsActions.get.promise(promise);
  },
});

export default ExtractorsStore;
