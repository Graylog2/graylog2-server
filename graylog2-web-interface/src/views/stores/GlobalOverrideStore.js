// @flow strict
import Reflux from 'reflux';
import moment from 'moment';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { TimeRange } from 'views/logic/queries/Query';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { SearchExecutionStateActions, SearchExecutionStateStore } from './SearchExecutionStateStore';

export type GlobalOverrideActionsType = RefluxActions<{
  rangeType: (string) => Promise<?GlobalOverride>,
  rangeParams: (string, string | number) => Promise<?GlobalOverride>,
  query: (string) => Promise<?GlobalOverride>,
  reset: () => Promise<?GlobalOverride>,
  timerange: (TimeRange) => Promise<?GlobalOverride>,
}>;

export const GlobalOverrideActions: GlobalOverrideActionsType = singletonActions(
  'views.GlobalOverride',
  () => Reflux.createActions({
    rangeType: { asyncResult: true },
    rangeParams: { asyncResult: true },
    query: { asyncResult: true },
    reset: { asyncResult: true },
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
    timerange(newTimerange: TimeRange) {
      const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
      const newGlobalOverride = currentGlobalOverride.toBuilder().timerange(newTimerange).build();

      const promise = this._propagateNewGlobalOverride(newGlobalOverride);
      GlobalOverrideActions.timerange.promise(promise);
      return promise;
    },
    rangeType(newType: string) {
      if (newType === 'disabled') {
        const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
        const newGlobalOverride: ?GlobalOverride = currentGlobalOverride.toBuilder().timerange(undefined).build();
        const promise = this._propagateNewGlobalOverride(newGlobalOverride);
        GlobalOverrideActions.rangeType.promise(promise);
        return promise;
      }
      const oldTimerange = this.globalOverride && this.globalOverride.timerange ? this.globalOverride.timerange : {};
      const { type: oldType } = oldTimerange;
      if (oldType !== newType) {
        let newTimerange;
        // eslint-disable-next-line default-case
        switch (newType) {
          case 'absolute':
            newTimerange = {
              type: newType,
              from: moment().subtract(oldTimerange.range || 300, 'seconds').toISOString(),
              to: moment().toISOString(),
            };
            break;
          case 'relative':
            newTimerange = {
              type: newType,
              range: 300,
            };
            break;
          case 'keyword':
            newTimerange = {
              type: newType,
              keyword: 'Last five Minutes',
            };
            break;
        }

        const promise = this.timerange(newTimerange);
        GlobalOverrideActions.rangeType.promise(promise);
        return promise;
      }
      const promise = Promise.resolve(this.globalOverride);
      GlobalOverrideActions.rangeType.promise(promise);
      return promise;
    },
    rangeParams(key: string, value: string | number) {
      const newTimerange: TimeRange = this.globalOverride && this.globalOverride.timerange
        ? { ...this.globalOverride.timerange, [key]: value }
        // $FlowFixMe: Flow is unable to validate that timerange is complete
        : { [key]: value };

      const promise = this.timerange(newTimerange);
      GlobalOverrideActions.rangeParams.promise(promise);
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
      const newGlobalOverride: GlobalOverride = this.globalOverride ? new GlobalOverride(this.globalOverride.newTimerange, newQuery) : new GlobalOverride(undefined, newQuery);
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
