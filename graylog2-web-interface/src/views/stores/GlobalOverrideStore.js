// @flow strict
import Reflux from 'reflux';
import moment from 'moment';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { GlobalOverride } from 'views/logic/search/SearchExecutionState';
import type { RefluxActions } from './StoreTypes';
import { SearchExecutionStateStore, SearchExecutionStateActions } from './SearchExecutionStateStore';

export type GlobalOverrideActionsType = RefluxActions<{
  rangeType: (string) => Promise<?GlobalOverride>,
  rangeParams: (string, string | number) => Promise<?GlobalOverride>,
  query: (string) => Promise<?GlobalOverride>,
}>;

export const GlobalOverrideActions: GlobalOverrideActionsType = singletonActions(
  'views.GlobalOverride',
  () => Reflux.createActions({
    rangeType: { asyncResult: true },
    rangeParams: { asyncResult: true },
    query: { asyncResult: true },
  }),
);

export const GlobalOverrideStore = singletonStore(
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
    rangeType(newType: string) {
      console.log('rangeType: ', newType, this.globalOverride);
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
        const newGlobalOverride = this.globalOverride ? { ...this.globalOverride, timerange: newTimerange } : { timerange: newTimerange };
        const promise = newGlobalOverride !== this.globalOverride
          ? SearchExecutionStateActions.globalOverride(newGlobalOverride).then(() => newGlobalOverride)
          : Promise.resolve(this.globalOverride);
        GlobalOverrideActions.timerange.promise(promise);
        return promise;
      }
      const promise = Promise.resolve(this.globalOverride);
      GlobalOverrideActions.rangeType.promise(promise);
      return promise;
    },
    rangeParams(key: string, value: string | number) {
      const newTimerange = this.globalOverride && this.globalOverride.timerange ? { ...this.globalOverride.timerange, [key]: value } : { [key]: value };
      const newGlobalOverride = this.globalOverride ? { ...this.globalOverride, timerange: newTimerange } : { timerange: newTimerange };
      const promise = newGlobalOverride !== this.globalOverride
        ? SearchExecutionStateActions.globalOverride(newGlobalOverride).then(() => newGlobalOverride)
        : Promise.resolve(this.globalOverride);
      GlobalOverrideActions.rangeParams.promise(promise);
      return promise;
    },
    query(newQueryString: string) {
      const newQuery = {
        type: 'elasticsearch',
        query_string: newQueryString,
      };
      const newGlobalOverride = this.globalOverride ? { ...this.globalOverride, query: newQuery } : { query: newQuery };
      const promise = newGlobalOverride !== this.globalOverride
        ? SearchExecutionStateActions.globalOverride(newGlobalOverride).then(() => newGlobalOverride)
        : Promise.resolve(this.globalOverride);
      GlobalOverrideActions.query.promise(promise);
      return promise;
    },
  }),
);
