import Reflux from 'reflux';
import Immutable from 'immutable';

import { SearchParameterStore } from './SearchParameterStore';
import { ViewMetadataStore } from './ViewMetadataStore';

const defaultExecutionState = Immutable.fromJS({
  parameter_bindings: {},
});

const newParameterBindingValue = value => Immutable.fromJS({
  type: 'value',
  value: value,
});

const getParameterBindings = executionState => executionState.get('parameter_bindings');
const getParameterBindingValue = (executionState, parameterName) => executionState.getIn(['parameter_bindings', parameterName, 'value']);

const getParameterBindingsAsMap = bindings => bindings.flatMap((value, name) => ({ [name]: value.get('value') }));

export { newParameterBindingValue, getParameterBindings, getParameterBindingsAsMap, getParameterBindingValue };

export const SearchExecutionStateActions = Reflux.createActions({
  bindParameterValue: { asyncResult: true },
  setParameterValues: { asyncResult: true },
  replace: { asyncResult: true },
  clear: { asyncResult: true },
});

export const SearchExecutionStateStore = Reflux.createStore({
  listenables: [SearchExecutionStateActions],

  executionState: defaultExecutionState,

  init() {
    this.listenTo(SearchParameterStore, this.handleSearchParameterChange, this.handleSearchParameterChange);
    this.listenTo(ViewMetadataStore, this.handleViewMetadataChange, this.handleViewMetadataChange);
  },

  getInitialState() {
    return this.executionState;
  },

  handleViewMetadataChange({ id }) {
    if (this.viewId !== id) {
      this.clear();
      this.viewId = id;
    }
  },

  handleSearchParameterChange(parameters) {
    const bindings = getParameterBindings(this.executionState);

    if (bindings) {
      // Cleanup the parameter bindings to only keep declared parameters
      const filteredBindings = bindings.filter((_, name) => parameters.has(name));
      this.executionState = this.executionState.set('parameter_bindings', filteredBindings);
      this.trigger(this.executionState);
    }
  },

  clear() {
    this.executionState = defaultExecutionState;
    this.trigger(this.executionState);
    SearchExecutionStateActions.clear.promise(Promise.resolve(this.executionState));
  },

  replace(executionState, trigger = true) {
    this.executionState = executionState;
    if (trigger) {
      this.trigger(this.executionState);
    }
    SearchExecutionStateActions.replace.promise(Promise.resolve(executionState));
  },

  setParameterValues(parameterMap) {
    parameterMap.forEach((value, parameterName) => {
      this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], newParameterBindingValue(value));
    });
    this.trigger(this.executionState);
    SearchExecutionStateActions.setParameterValues.promise(Promise.resolve(this.executionState));
    return this.executionState;
  },

  bindParameterValue(parameterName, value) {
    this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], newParameterBindingValue(value));
    this.trigger(this.executionState);
    SearchExecutionStateActions.bindParameterValue.promise(Promise.resolve(this.executionState));
  },
});
