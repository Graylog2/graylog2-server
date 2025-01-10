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

import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ExtractorUtils from 'util/ExtractorUtils';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { singletonStore, singletonActions } from 'logic/singleton';

export type InputSummary = {
  creator_user_id: string;
  node: string;
  name: string;
  created_at: string;
  global: boolean;
  attributes: {
    [_key: string]: Object;
  };
  id: string;
  title: string;
  type: string;
  content_pack: string;
  static_fields: {
    [_key: string]: string;
  };
};

export type NodeSummary = {
  cluster_id: string,
  hostname: string,
  is_leader: boolean,
  is_master: boolean,
  last_seen: string,
  node_id: string,
  short_node_id: string,
  transport_address: string,
  type: string,
};

type RateMetricsResponse = {
  total: number,
  mean: number,
  five_minute: number,
  fifteen_minute: number,
  one_minute: number,
};

type TimerMetricsResponse = {
  min: number,
  max: number,
  std_dev: number,
  mean: number,
  '95th_percentile': number,
  '99th_percentile': number,
  '98th_percentile': number,
};

type TimerRateMetricsResponse = {
  rate: RateMetricsResponse,
  rate_unit: string,
  time: TimerMetricsResponse,
  duration_unit: string,
};

export type ExtractorMetrics = {
  condition_misses: number,
  execution: TimerRateMetricsResponse,
  total: TimerRateMetricsResponse,
  condition: TimerRateMetricsResponse,
  condition_hits: number,
  converters: TimerRateMetricsResponse,
};

export type ExtractorType = {
  creator_user_id: string,
  extractor_type?: string,
  source_field: string,
  condition_type: string,
  converter_exceptions: number,
  title: string,
  type: string,
  cursor_strategy: string,
  exceptions: number,
  target_field: string,
  extractor_config: {
    [_key: string]: Object,
  },
  condition_value: string,
  converters: {
    [_key: string]: Object,
  }[],
  id?: string,
  metrics: ExtractorMetrics,
  order: number,
};

type ExtractorsActionsType = {
  list: (inputId: string) => Promise<Array<ExtractorType>>,
  get: (inputId: string, extractorId: string) => Promise<ExtractorType>,
  create: (inputId: string, extractor: ExtractorType, calledFromMethod: boolean) => Promise<unknown>,
  save: (inputId: string, extractor: ExtractorType) => Promise<unknown>,
  update: (inputId: string, extractor: ExtractorType, calledFromMethod: boolean) => Promise<unknown>,
  delete: (inputId: string, extractor: ExtractorType) => Promise<unknown>,
  order: (inputId: string, orderedExtractors: Array<ExtractorType>) => Promise<unknown>,
  import: (inputId: string, orderedExtractors: Array<ExtractorType>) => Promise<unknown>,
};

export type ExtractorsStoreState = {
  extractors: Array<ExtractorType>,
  extractor: ExtractorType,
};

export const ExtractorsActions = singletonActions(
  'core.Extractors',
  () => Reflux.createActions<ExtractorsActionsType>({
    list: { asyncResult: true },
    get: { asyncResult: true },
    create: { asyncResult: true },
    save: { asyncResult: true },
    update: { asyncResult: true },
    delete: { asyncResult: true },
    order: { asyncResult: true },
    import: { asyncResult: true },
  }),
);

function getExtractorDTO(extractor: ExtractorType) {
  const conditionValue = extractor.condition_type && extractor.condition_type !== 'none' ? extractor.condition_value : '';

  return {
    title: extractor.title,
    cursor_strategy: extractor.cursor_strategy || 'copy',
    source_field: extractor.source_field,
    target_field: extractor.target_field,
    extractor_type: extractor.type || extractor.extractor_type, // "extractor_type" needed for imports
    extractor_config: extractor.extractor_config,
    converters: extractor.converters,
    condition_type: extractor.condition_type || 'none',
    condition_value: conditionValue,
    order: extractor.order,
  };
}

export const ExtractorsStore = singletonStore(
  'core.Extractors',
  () => Reflux.createStore<ExtractorsStoreState>({
    listenables: [ExtractorsActions],
    sourceUrl: '/system/inputs/',
    extractors: undefined,
    extractor: undefined,

    getInitialState() {
      return this.getState();
    },

    init() {
      this.trigger({ extractors: this.extractors, extractor: this.extractor });
    },

    getState() {
      return {
        extractors: this.extractors,
        extractor: this.extractor,
      };
    },

    propagateState() {
      this.trigger(this.getState());
    },

    list(inputId: string) {
      const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors')));

      promise.then((response) => {
        this.extractors = response.extractors;
        this.propagateState();
      });

      ExtractorsActions.list.promise(promise);
    },

    // Creates an basic extractor object that we can use to create new extractors.
    new(type: string, field: string) {
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

    get(inputId: string, extractorId: string) {
      const promise = fetch('GET', URLUtils.qualifyUrl(URLUtils.concatURLPath(this.sourceUrl, inputId, 'extractors', extractorId)));

      promise.then((response) => {
        this.extractor = response;
        this.propagateState();
      });

      ExtractorsActions.get.promise(promise);
    },

    save(inputId: string, extractor: ExtractorType) {
      let promise;

      if (extractor.id) {
        promise = this.update(inputId, extractor, true);
      } else {
        promise = this.create(inputId, extractor, true);
      }

      ExtractorsActions.save.promise(promise);
    },

    _silentExtractorCreate(inputId: string, extractor: ExtractorType) {
      const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.create(inputId).url);

      return fetch('POST', url, getExtractorDTO(extractor));
    },

    create(inputId: string, extractor: ExtractorType, calledFromMethod: boolean) {
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

    update(inputId: string, extractor: ExtractorType, calledFromMethod: boolean) {
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

    delete(inputId: string, extractor: ExtractorType) {
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

    order(inputId: string, orderedExtractors: Array<ExtractorType>) {
      const url = URLUtils.qualifyUrl(ApiRoutes.ExtractorsController.order(inputId).url);
      const orderedExtractorsMap = {};

      orderedExtractors.forEach((extractor, idx) => {
        orderedExtractorsMap[idx] = extractor.id;
      });

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

    import(inputId: string, extractors: Array<ExtractorType>) {
      let successfulImports = 0;
      let failedImports = 0;
      const promises = [];

      extractors.forEach((extractor) => {
        const promise = this._silentExtractorCreate(inputId, extractor);

        promise
          .then(() => { successfulImports += 1; })
          .catch(() => { failedImports += 1; });

        promises.push(promise);
      });

      Promise.allSettled(promises).then(() => {
        if (failedImports === 0) {
          UserNotification.success(`Import results: ${successfulImports} extractor(s) imported.`,
            'Import operation successful');

          this.propagateState();
        } else {
          UserNotification.warning(`Import results: ${successfulImports} extractor(s) imported, ${failedImports} error(s).`,
            'Import operation completed');
        }
      });
    },
  }),
);
