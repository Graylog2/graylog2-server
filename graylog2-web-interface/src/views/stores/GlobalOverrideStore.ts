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

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { ElasticsearchQueryString, TimeRange } from 'views/logic/queries/Query';
import type { RefluxActions, Store } from 'stores/StoreTypes';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';

import { SearchExecutionStateActions, SearchExecutionStateStore } from './SearchExecutionStateStore';

export type GlobalOverrideStoreState = GlobalOverride | undefined;

export type GlobalOverrideActionsType = RefluxActions<{
  query: (newQueryString: string) => Promise<GlobalOverrideStoreState>,
  set: (newTimeRange?: TimeRange, newQueryString?: string) => Promise<GlobalOverrideStoreState>,
  reset: () => Promise<GlobalOverrideStoreState>,
  timerange: (newTimeRange?: TimeRange) => Promise<GlobalOverrideStoreState>,
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
      console.log('override', this.globalOverride);

      return this.globalOverride;
    },
    set(newTimeRange: TimeRange | undefined, newQueryString?: string): Promise<GlobalOverride | undefined> {
      console.log({ newTimeRange });
      const newQuery = newQueryString ? createElasticsearchQueryString(newQueryString) : undefined;
      const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
      const newGlobalOverride = currentGlobalOverride.toBuilder().query(newQuery).timerange(newTimeRange).build();

      const promise = this._propagateNewGlobalOverride(newGlobalOverride);

      GlobalOverrideActions.set.promise(promise);

      return promise;
    },
    timerange(newTimeRange: TimeRange | undefined) {
      const currentGlobalOverride = this.globalOverride || GlobalOverride.empty();
      const newGlobalOverride = currentGlobalOverride.toBuilder().timerange(newTimeRange).build();

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
      const newQuery: ElasticsearchQueryString = {
        type: 'elasticsearch',
        query_string: newQueryString,
      };
      const newGlobalOverride: GlobalOverride = this.globalOverride ? new GlobalOverride(this.globalOverride.timerange, newQuery) : new GlobalOverride(undefined, newQuery);
      const promise = this._propagateNewGlobalOverride(newGlobalOverride);

      GlobalOverrideActions.query.promise(promise);

      return promise;
    },
    _propagateNewGlobalOverride(newGlobalOverride: GlobalOverride | undefined) {
      return newGlobalOverride !== this.globalOverride
        ? SearchExecutionStateActions.globalOverride(newGlobalOverride).then(() => newGlobalOverride)
        : Promise.resolve(this.globalOverride);
    },
  }),
);
