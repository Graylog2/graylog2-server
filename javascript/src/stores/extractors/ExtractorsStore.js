import Reflux from 'reflux';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';
import ExtractorsActions from 'actions/extractors/ExtractorsActions';
import ExtractorUtils from 'util/ExtractorUtils';

import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

function getExtractorDTO(extractor) {
  const converters = {};
  extractor.converters.forEach(converter => {
    converters[converter.type] = converter.config;
  });

  const conditionValue = extractor.condition_type && extractor.condition_type !== 'none' ? extractor.condition_value : '';

  return {
    title: extractor.title,
    cut_or_copy: extractor.cursor_strategy || 'copy',
    source_field: extractor.source_field,
    target_field: extractor.target_field,
    extractor_type: extractor.type,
    extractor_config: extractor.extractor_config,
    converters: converters,
    condition_type: extractor.condition_type || 'none',
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

  // Creates an basic extractor object that we can use to create new extractors.
  new(type, field) {
    if (ExtractorUtils.EXTRACTOR_TYPES.indexOf(type) === -1) {
      throw new Error('Invalid extractor type provided: ' + type);
    }

    return {
      type: type,
      source_field: field,
      converters: [],
      extractor_config: {},
    };
  },

  get(inputId, extractorId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors', extractorId)))
      .then(response => {
        this.extractor = response;
        this.trigger({extractor: this.extractor});
      });

    ExtractorsActions.get.promise(promise);
  },

  save(inputId, extractor) {
    let promise;

    if (extractor.id) {
      promise = this.update(inputId, extractor, true);
    } else {
      promise = this.create(inputId, extractor, true);
    }

    ExtractorsActions.save.promise(promise);
  },

  create(inputId, extractor, calledFromMethod) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.ExtractorsController.create(inputId).url);

    const promise = fetch('POST', url, getExtractorDTO(extractor))
      .then(() => {
        UserNotification.success(`Extractor ${extractor.title} created successfully`);
        if (this.extractor) {
          ExtractorsActions.get.triggerPromise(inputId, extractor.id);
        }
      })
      .catch(error => {
        UserNotification.error('Creating extractor failed: ' + error,
          'Could not create extractor');
      });

    if (!calledFromMethod) {
      ExtractorsActions.create.promise(promise);
    }
    return promise;
  },

  update(inputId, extractor, calledFromMethod) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.ExtractorsController.update(inputId, extractor.id).url);

    const promise = fetch('PUT', url, getExtractorDTO(extractor))
      .then(() => {
        UserNotification.success(`Extractor "${extractor.title}" updated successfully`);
        if (this.extractor) {
          ExtractorsActions.get.triggerPromise(inputId, extractor.id);
        }
      })
      .catch(error => {
        UserNotification.error('Updating extractor failed: ' + error,
          'Could not update extractor');
      });

    if (!calledFromMethod) {
      ExtractorsActions.update.promise(promise);
    }
    return promise;
  },

  delete(inputId, extractor) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.ExtractorsController.delete(inputId, extractor.id).url);

    const promise = fetch('DELETE', url)
      .then(() => {
        UserNotification.success(`Extractor "${extractor.title}" deleted successfully`);
        if (this.extractors) {
          ExtractorsActions.list.triggerPromise(inputId);
        }
      })
      .catch(error => {
        UserNotification.error('Deleting extractor failed: ' + error,
          `Could not delete extractor ${extractor.title}`);
      });

    ExtractorsActions.delete.promise(promise);
  },
});

export default ExtractorsStore;
