// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import URLUtils from 'util/URLUtils';

import type { RefluxActions } from 'stores/StoreTypes';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import { QueryFiltersStore } from './QueryFiltersStore';

const fieldTypesUrl = URLUtils.qualifyUrl('/views/fields');

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

export const FieldTypesStore = singletonStore(
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
      const promises = newFilters
        .filter(filter => filter !== undefined && filter !== null)
        .map(filter => filter.get('filters', Immutable.List()).filter(f => f.get('type') === 'stream').map(f => f.get('id')))
        .filter(streamFilters => streamFilters.size > 0)
        .map((filters, queryId) => this.forStreams(filters.toArray()).then(response => ({
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
        .map(fieldTypeMapping => FieldTypeMapping.fromJSON(fieldTypeMapping));
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
