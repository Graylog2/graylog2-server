// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { singletonActions, singletonStore } from 'views/logic/singleton';
import type { RefluxActions } from './StoreTypes';

const defaultExecutionState = SearchExecutionState.empty();

type ParameterMap = Immutable.Map<string, any>;

export type SearchExecutionStateActionsType = RefluxActions<{
  bindParameterValue: (string, any) => Promise<SearchExecutionState>,
  setParameterValues: (ParameterMap) => Promise<SearchExecutionState>,
  replace: (SearchExecutionState, ?boolean) => Promise<SearchExecutionState>,
  clear: () => Promise<SearchExecutionState>,
}>;

export const SearchExecutionStateActions: SearchExecutionStateActionsType = singletonActions(
  'views.SearchExecutionState',
  () => Reflux.createActions({
    bindParameterValue: { asyncResult: true },
    setParameterValues: { asyncResult: true },
    replace: { asyncResult: true },
    clear: { asyncResult: true },
  }),
);

export const SearchExecutionStateStore = singletonStore(
  'views.SearchExecutionState',
  () => Reflux.createStore({
    listenables: [SearchExecutionStateActions],

    executionState: defaultExecutionState,

    init() {
      this.listenTo(ViewMetadataStore, this.handleViewMetadataChange, this.handleViewMetadataChange);
    },

    getInitialState(): SearchExecutionState {
      return this.executionState;
    },

    handleViewMetadataChange({ id }) {
      if (this.viewId !== id) {
        this.clear();
        this.viewId = id;
      }
    },

    clear(): SearchExecutionState {
      this.executionState = defaultExecutionState;
      this.trigger(this.executionState);
      SearchExecutionStateActions.clear.promise(Promise.resolve(this.executionState));
      return this.executionState;
    },

    replace(executionState: SearchExecutionState, trigger?: boolean = true): SearchExecutionState {
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

    bindParameterValue(parameterName: String, value: any): SearchExecutionState {
      this.executionState = this.executionState.toBuilder()
        .parameterBindings(this.executionState.parameterBindings.set(parameterName, ParameterBinding.forValue(value)))
        .build();
      this.trigger(this.executionState);
      SearchExecutionStateActions.bindParameterValue.promise(Promise.resolve(this.executionState));
      return this.executionState;
    },
  }),
);
