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
// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { singletonActions, singletonStore } from 'views/logic/singleton';

import { QueryFiltersStore } from './QueryFiltersStore';

const fieldTypesUrl = qualifyUrl('/views/fields');

type FieldTypesActionsType = RefluxActions<{
  all: () => Promise<void>,
}>;

export const FieldTypesActions: FieldTypesActionsType = singletonActions(
  'views.FieldTypes',
  () => Reflux.createActions({
    all: { asyncResult: true },
  }),
);

export type FieldTypeMappingsList = Immutable.List<FieldTypeMapping>;
export type FieldTypesStoreState = {
  all: FieldTypeMappingsList,
  queryFields: Immutable.Map<string, FieldTypeMappingsList>,
};

export type FieldTypesStoreType = Store<FieldTypesStoreState>;

export const FieldTypesStore: FieldTypesStoreType = singletonStore(
  'views.FieldTypes',
  () => Reflux.createStore({
    listenables: [FieldTypesActions],

    _all: Immutable.List<FieldTypeMapping>(),
    _queryFields: Immutable.Map<String, FieldTypeMappingsList>(),

    init() {
      this.all();
      this.listenTo(QueryFiltersStore, this.onQueryFiltersUpdate, this.onQueryFiltersUpdate);
    },

    getInitialState() {
      return this._state();
    },

    onQueryFiltersUpdate(newFilters) {
      const streamIds = newFilters
        .map((filter) => filter.get('filters', Immutable.List()).filter((f) => f.get('type') === 'stream').map((f) => f.get('id')));
      const promises = streamIds
        .map((filters, queryId) => (filters.size > 0
          ? this.forStreams(filters.toArray())
          : Promise.resolve(this._all))
          .then((response) => ({
            queryId,
            response,
          })))
        .valueSeq()
        .toJS();

      Promise.all(promises).then((results) => {
        const combinedResult = {};

        results.forEach(({ queryId, response }) => {
          combinedResult[queryId] = response;
        });

        this._queryFields = Immutable.fromJS(combinedResult);
        this._trigger();
      });
    },

    all() {
      const promise = fetch('GET', fieldTypesUrl)
        .then(this._deserializeFieldTypes)
        .then((response) => {
          this._all = Immutable.fromJS(response);
          this._trigger();
        });

      FieldTypesActions.all.promise(promise);

      return promise;
    },

    forStreams(streams) {
      return fetch('POST', fieldTypesUrl, { streams: streams })
        .then(this._deserializeFieldTypes);
    },

    _deserializeFieldTypes(response) {
      return response
        .map((fieldTypeMapping) => FieldTypeMapping.fromJSON(fieldTypeMapping));
    },

    _state(): FieldTypesStoreState {
      return {
        all: this._all,
        queryFields: this._queryFields,
      };
    },
    _trigger() {
      this.trigger(this._state());
    },
  }),
);
