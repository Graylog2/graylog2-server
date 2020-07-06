// @flow strict
import Reflux from 'reflux';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { TimeRange } from 'views/logic/queries/Query';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import { SearchExecutionStateActions, SearchExecutionStateStore } from './SearchExecutionStateStore';

export type GlobalOverrideActionsType = RefluxActions<{
  query: (string) => Promise<?GlobalOverride>,
  set: (?TimeRange, ?string) => Promise<?GlobalOverride>,
  reset: () => Promise<?GlobalOverride>,
  timerange: (?TimeRange) => Promise<?GlobalOverride>,
}>;

export const GlobalOverrideActions: GlobalOverrideActionsType = singletonActions(
  'views.GlobalOverride',
  () => Reflux.createActions({
    query: { asyncResult: true },
    reset: { asyncResult: true },
    set: { asyncResult: true },
    timerange: { asyncResult: true },
  }),
);

type GlobalOverrideStoreState = ?GlobalOverride;
type GlobalOverrideStoreType = Store<GlobalOverrideStoreState>;

export const GlobalOverrideStore: GlobalOverrideStoreType = singletonStore(
  'views.GlobalOverride',
  () => Reflux.createStore({
    listenables: [GlobalOverrideActions],
    globalOverride: undefined,
    init() {
      this.listenTo(SearchExecutionStateStore, this.handleSearchExecutionStateStoreChange, this.handleSearchExecutionStateStoreChange);
    },
    handleSearchExecutionStateStoreChange(newSearchExecutionState: SearchExecutionState) {
      if (newSearchExecutionState.globalOverride !== this.globalOverride) {
        this.globalOverride = newSearchExecutionState.globalOverride;
        this.trigger(this.globalOverride);
      }
    },
    getInitialState() {
      return this.globalOverride;
    },
    set(newTimerange: ?TimeRange, newQueryString: ?string): Promise<?GlobalOverride> {
      const newQuery = newQueryString ? createElasticsearchQueryString(newQueryString) : undefined;
      const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
      const newGlobalOverride = currentGlobalOverride.toBuilder().query(newQuery).timerange(newTimerange).build();

      const promise = this._propagateNewGlobalOverride(newGlobalOverride);

      GlobalOverrideActions.set.promise(promise);

      return promise;
    },
    timerange(newTimerange: TimeRange) {
      const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
      const newGlobalOverride = currentGlobalOverride.toBuilder().timerange(newTimerange).build();

      const promise = this._propagateNewGlobalOverride(newGlobalOverride);

      GlobalOverrideActions.timerange.promise(promise);

      return promise;
    },
    reset() {
      const promise = this._propagateNewGlobalOverride(undefined);

      GlobalOverrideActions.reset.promise(promise);

      return promise;
    },
    query(newQueryString: string) {
      const newQuery = {
        type: 'elasticsearch',
        query_string: newQueryString,
      };
      const newGlobalOverride: GlobalOverride = this.globalOverride ? new GlobalOverride(this.globalOverride.timerange, newQuery) : new GlobalOverride(undefined, newQuery);
      const promise = this._propagateNewGlobalOverride(newGlobalOverride);

      GlobalOverrideActions.query.promise(promise);

      return promise;
    },
    _propagateNewGlobalOverride(newGlobalOverride: ?GlobalOverride) {
      return newGlobalOverride !== this.globalOverride
        ? SearchExecutionStateActions.globalOverride(newGlobalOverride).then(() => newGlobalOverride)
        : Promise.resolve(this.globalOverride);
    },
  }),
);
