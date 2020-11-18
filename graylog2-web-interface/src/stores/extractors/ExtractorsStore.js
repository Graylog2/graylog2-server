/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';
import Promise from 'bluebird';

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';
import ExtractorUtils from 'util/ExtractorUtils';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const ExtractorsActions = ActionsProvider.getActions('Extractors');

function getExtractorDTO(extractor) {
  const converters = {};

  extractor.converters.forEach((converter) => {
    converters[converter.type] = converter.config;
  });

  const conditionValue = extractor.condition_type && extractor.condition_type !== 'none' ? extractor.condition_value : '';

  return {
    title: extractor.title,
    cut_or_copy: extractor.cursor_strategy || 'copy',
    source_field: extractor.source_field,
    target_field: extractor.target_field,
    extractor_type: extractor.type || extractor.extractor_type, // "extractor_type" needed for imports
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
    this.trigger({ extractors: this.extractors, extractor: this.extractor });
  },

  list(inputId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors')));

    promise.then((response) => {
      this.extractors = response.extractors;
      this.trigger({ extractors: this.extractors });
    });

    ExtractorsActions.list.promise(promise);
  },

  // Creates an basic extractor object that we can use to create new extractors.
  new(type, field) {
    if (ExtractorUtils.EXTRACTOR_TYPES.indexOf(type) === -1) {
      throw new Error(`Invalid extractor type provided: ${type}`);
    }

    return {
      type: type,
      source_field: field,
      converters: [],
      extractor_config: {},
      target_field: '',
    };
  },

  get(inputId, extractorId) {
    const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors', extractorId)));

    promise.then((response) => {
      this.extractor = response;
      this.trigger({ extractor: this.extractor });
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

  _silentExtractorCreate(inputId, extractor) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.create(inputId).url);

    return fetch('POST', url, getExtractorDTO(extractor));
  },

  create(inputId, extractor, calledFromMethod) {
    const promise = this._silentExtractorCreate(inputId, extractor);

    promise
      .then(() => {
        UserNotification.success(`Extractor ${extractor.title} created successfully`);

        if (this.extractor) {
          ExtractorsActions.get.triggerPromise(inputId, extractor.id);
        }
      })
      .catch((error) => {
        UserNotification.error(`Creating extractor failed: ${error}`,
          'Could not create extractor');
      });

    if (!calledFromMethod) {
      ExtractorsActions.create.promise(promise);
    }

    return promise;
  },

  update(inputId, extractor, calledFromMethod) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.update(inputId, extractor.id).url);

    const promise = fetch('PUT', url, getExtractorDTO(extractor));

    promise
      .then(() => {
        UserNotification.success(`Extractor "${extractor.title}" updated successfully`);

        if (this.extractor) {
          ExtractorsActions.get.triggerPromise(inputId, extractor.id);
        }
      })
      .catch((error) => {
        UserNotification.error(`Updating extractor failed: ${error}`,
          'Could not update extractor');
      });

    if (!calledFromMethod) {
      ExtractorsActions.update.promise(promise);
    }

    return promise;
  },

  delete(inputId, extractor) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.delete(inputId, extractor.id).url);

    const promise = fetch('DELETE', url);

    promise
      .then(() => {
        UserNotification.success(`Extractor "${extractor.title}" deleted successfully`);

        if (this.extractors) {
          ExtractorsActions.list.triggerPromise(inputId);
        }
      })
      .catch((error) => {
        UserNotification.error(`Deleting extractor failed: ${error}`,
          `Could not delete extractor ${extractor.title}`);
      });

    ExtractorsActions.delete.promise(promise);
  },

  order(inputId, orderedExtractors) {
    const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.order(inputId).url);
    const orderedExtractorsMap = {};

    orderedExtractors.forEach((extractor, idx) => orderedExtractorsMap[idx] = extractor.id);

    const promise = fetch('POST', url, { order: orderedExtractorsMap });

    promise.then(() => {
      UserNotification.success('Extractor positions updated successfully');

      if (this.extractors) {
        ExtractorsActions.list.triggerPromise(inputId);
      }
    });

    promise.catch((error) => {
      UserNotification.error(`Changing extractor positions failed: ${error}`,
        'Could not update extractor positions');
    });

    ExtractorsActions.order.promise(promise);
  },

  import(inputId, extractors) {
    let successfulImports = 0;
    let failedImports = 0;
    const promises = [];

    extractors.forEach((extractor) => {
      const promise = this._silentExtractorCreate(inputId, extractor);

      promise
        .then(() => successfulImports++)
        .catch(() => failedImports++);

      promises.push(promise);
    });

    Promise.settle(promises).then(() => {
      if (failedImports === 0) {
        UserNotification.success(`Import results: ${successfulImports} extractor(s) imported.`,
          'Import operation successful');
      } else {
        UserNotification.warning(`Import results: ${successfulImports} extractor(s) imported, ${failedImports} error(s).`,
          'Import operation completed');
      }
    });
  },
});

export default ExtractorsStore;
