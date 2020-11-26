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

import type { RefluxActions, Store } from 'stores/StoreTypes';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import GlobalOverride from 'views/logic/search/GlobalOverride';

import { ViewActions } from './ViewStore';

const defaultExecutionState = SearchExecutionState.empty();

type ParameterMap = Immutable.Map<string, any>;

export type SearchExecutionStateActionsType = RefluxActions<{
  bindParameterValue: (name: string, value: any) => Promise<SearchExecutionState>,
  setParameterValues: (parameters: ParameterMap) => Promise<SearchExecutionState>,
  replace: (state: SearchExecutionState, trigger?: boolean) => Promise<SearchExecutionState>,
  clear: () => Promise<SearchExecutionState>,
  globalOverride: (globalOverride?: GlobalOverride) => Promise<SearchExecutionState>,
}>;

export const SearchExecutionStateActions: SearchExecutionStateActionsType = singletonActions(
  'views.SearchExecutionState',
  () => Reflux.createActions({
    bindParameterValue: { asyncResult: true },
    setParameterValues: { asyncResult: true },
    replace: { asyncResult: true },
    clear: { asyncResult: true },
    globalOverride: { asyncResult: true },
  }),
);

export const SearchExecutionStateStore: Store<SearchExecutionState> = singletonStore(
  'views.SearchExecutionState',
  () => Reflux.createStore({
    listenables: [SearchExecutionStateActions],

    executionState: defaultExecutionState,

    init() {
      ViewActions.create.completed.listen(this.clear);
      ViewActions.load.completed.listen(this.clear);
    },

    getInitialState(): SearchExecutionState {
      return this.executionState;
    },

    clear(): SearchExecutionState {
      this.executionState = defaultExecutionState;
      this.trigger(this.executionState);
      SearchExecutionStateActions.clear.promise(Promise.resolve(this.executionState));

      return this.executionState;
    },

    replace(executionState: SearchExecutionState, trigger = true): SearchExecutionState {
      this.executionState = executionState;

      if (trigger) {
        this.trigger(this.executionState);
      }

      SearchExecutionStateActions.replace.promise(Promise.resolve(executionState));

      return this.executionState;
    },

    setParameterValues(parameterMap: ParameterMap): SearchExecutionState {
      let { parameterBindings } = this.executionState;

      parameterMap.forEach((value, parameterName) => {
        parameterBindings = parameterBindings.set(parameterName, ParameterBinding.forValue(value));
      });

      this.executionState = this.executionState.toBuilder().parameterBindings(parameterBindings).build();
      this.trigger(this.executionState);
      SearchExecutionStateActions.setParameterValues.promise(Promise.resolve(this.executionState));

      return this.executionState;
    },

    bindParameterValue(parameterName: string, value: any): SearchExecutionState {
      this.executionState = this.executionState.toBuilder()
        .parameterBindings(this.executionState.parameterBindings.set(parameterName, ParameterBinding.forValue(value)))
        .build();

      this.trigger(this.executionState);
      SearchExecutionStateActions.bindParameterValue.promise(Promise.resolve(this.executionState));

      return this.executionState;
    },

    globalOverride(newGlobalOverride: GlobalOverride | undefined): SearchExecutionState {
      this.executionState = this.executionState.toBuilder()
        .globalOverride(newGlobalOverride)
        .build();

      this.trigger(this.executionState);
      SearchExecutionStateActions.globalOverride.promise(Promise.resolve(this.executionState));

      return this.executionState;
    },
  }),
);
