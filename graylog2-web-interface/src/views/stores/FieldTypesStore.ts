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
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import FieldTypeMapping, { FieldTypeMappingJSON } from 'views/logic/fieldtypes/FieldTypeMapping';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import Query, { TimeRange } from 'views/logic/queries/Query';

import { QueryFiltersStore } from './QueryFiltersStore';

const fieldTypesUrl = qualifyUrl('/views/fields');

type FieldTypesActionsType = RefluxActions<{
  all: () => Promise<void>,
  refresh: () => Promise<void>,
}>;

export const FieldTypesActions: FieldTypesActionsType = singletonActions(
  'views.FieldTypes',
  () => Reflux.createActions({
    all: { asyncResult: true },
    refresh: { asyncResult: true },
  }),
);

export type FieldTypeMappingsList = Immutable.List<FieldTypeMapping>;
export type FieldTypesStoreState = {
  all: FieldTypeMappingsList,
  queryFields: Immutable.Map<string, FieldTypeMappingsList>,
};

export type FieldTypesStoreType = Store<FieldTypesStoreState>;

type FieldTypesResponse = Array<FieldTypeMappingJSON>;

const _deserializeFieldTypes = (response: FieldTypesResponse) => response
  .map((fieldTypeMapping) => FieldTypeMapping.fromJSON(fieldTypeMapping));
const fetchAllFieldTypes = (timerange: TimeRange): Promise<Array<FieldTypeMapping>> => fetch('POST', fieldTypesUrl, { timerange })
  .then(_deserializeFieldTypes);

const streamIdsFromFilters = (filters: Immutable.Map<string, Immutable.Map<'filters', Immutable.List<Immutable.Map<string, string>>>>) => filters
  .map((filter) => filter.get('filters', Immutable.List())
    .filter((f) => f.get('type') === 'stream')
    .map((f) => f.get('id')));

export const FieldTypesStore: FieldTypesStoreType = singletonStore(
  'views.FieldTypes',
  () => Reflux.createStore({
    listenables: [FieldTypesActions],

    _all: Immutable.List<FieldTypeMapping>(),
    _queryFields: Immutable.Map<string, FieldTypeMappingsList>(),

    init() {
      this.all();
      this.listenTo(QueryFiltersStore, this.onQueryFiltersUpdate, this.onQueryFiltersUpdate);
      this.listenTo(CurrentQueryStore, this.onCurrentQueryUpdate, this.onCurrentQueryUpdate);
    },

    getInitialState() {
      return this._state();
    },

    onCurrentQueryUpdate(query: Query) {
      this._timerange = query?.timerange;
      this.refresh();
    },

    onQueryFiltersUpdate(newFilters) {
      this._streamIds = streamIdsFromFilters(newFilters);
      this.refresh();
    },

    all(timerange: TimeRange) {
      const promise = fetchAllFieldTypes(timerange)
        .then((response) => {
          this._all = Immutable.fromJS(response);
          this._trigger();
        });

      FieldTypesActions.all.promise(promise);

      return promise;
    },

    refresh() {
      const allFields = fetchAllFieldTypes(this._timerange);
      const promises = this._streamIds?.map((filters, queryId) => (filters.size > 0
        ? this.forStreams(filters, this._timerange)
        : allFields)
        .then((response) => ({
          queryId,
          response,
        })))
        .valueSeq()
        .toArray();

      Promise.all(promises).then((results) => {
        const combinedResult = {};

        results.forEach(({ queryId, response }) => {
          combinedResult[queryId] = response;
        });

        this._queryFields = Immutable.fromJS(combinedResult);
        this._trigger();
      });
    },

    forStreams(streams: Array<string>, timerange: TimeRange) {
      return fetch('POST', fieldTypesUrl, { streams, timerange })
        .then(_deserializeFieldTypes);
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
