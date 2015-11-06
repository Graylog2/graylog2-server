import Reflux from 'reflux';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

function getExtractorDTO(extractor) {
  const converters = {};
  extractor.converters.forEach(converter => {
    converters[converter.type] = converter.config;
  });

  const conditionValue = extractor.condition_type !== 'none' ? extractor.condition_value : '';

  return {
    title: extractor.title,
    cut_or_copy: extractor.cursor_strategy,
    source_field: extractor.source_field,
    target_field: extractor.target_field,
    extractor_type: extractor.type,
    extractor_config: extractor.extractor_config,
    converters: converters,
    condition_type: extractor.condition_type,
    condition_value: conditionValue,
    order: extractor.order,
  };
}

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

  update(inputId, extractor) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.ExtractorsController.update(inputId, extractor.id).url);

    const promise = fetch('PUT', url, getExtractorDTO(extractor))
      .then(() => {
        UserNotification.success('Extractor updated successfully');
        if (this.extractor) {
          ExtractorsActions.get.triggerPromise(inputId, extractor.id);
        }
      })
      .catch(error => {
        UserNotification.error('Updating extractor failed: ' + error,
          'Could not update extractor');
      });

    ExtractorsActions.update.promise(promise);
  },
});

export default ExtractorsStore;
