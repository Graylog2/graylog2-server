// @flow strict
import Reflux from 'reflux';
import * as Immutable from 'immutable';
import { trim } from 'lodash';

import SearchExecutionState from 'enterprise/logic/search/SearchExecutionState';
import Parameter from 'enterprise/logic/parameters/Parameter';
import ParameterBinding from 'enterprise/logic/parameters/ParameterBinding';
import { SearchParameterActions } from './SearchParameterStore';
import { ViewMetadataStore } from './ViewMetadataStore';

const defaultExecutionState = SearchExecutionState.empty();

type ParameterMap = Immutable.Map<string, any>;

export type SearchExecutionStateActionsType = {
  bindParameterValue: (string, any) => Promise<SearchExecutionState>,
  setParameterValues: (ParameterMap) => Promise<SearchExecutionState>,
  replace: (SearchExecutionState, ?boolean) => Promise<SearchExecutionState>,
  clear: () => Promise<SearchExecutionState>,
};

export const SearchExecutionStateActions: SearchExecutionStateActionsType = Reflux.createActions({
  bindParameterValue: { asyncResult: true },
  setParameterValues: { asyncResult: true },
  replace: { asyncResult: true },
  clear: { asyncResult: true },
});

export const SearchExecutionStateStore = Reflux.createStore({
  listenables: [SearchExecutionStateActions],

  executionState: defaultExecutionState,

  init() {
    this.listenTo(SearchParameterActions.remove, this._removeParameterBindingForRemovedParameter);
    this.listenTo(SearchParameterActions.update, this._updateParameterBindingForUpdatedParameter);
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

  _removeParameterBindingForRemovedParameter(parameterName: string) {
    const { parameterBindings } = this.executionState;
    if (parameterBindings.has(parameterName)) {
      const newParameterBindings = parameterBindings.delete(parameterName);
      this.executionState = this.executionState.toBuilder().parameterBindings(newParameterBindings).build();
      this.trigger(this.executionState);
    }
  },

  _updateParameterBindingForUpdatedParameter(parameterName: string, newParameter: Parameter) {
    const { parameterBindings } = this.executionState;
    if (!trim(parameterBindings.get(parameterName, ParameterBinding.empty()).value) && newParameter.defaultValue) {
      const newParameterBindings = parameterBindings.set(parameterName, ParameterBinding.forValue(newParameter.defaultValue));
      this.executionState = this.executionState.toBuilder().parameterBindings(newParameterBindings).build();
      this.trigger(this.executionState);
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
});
