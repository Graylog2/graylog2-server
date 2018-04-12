import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchExecutionStateActions from 'enterprise/actions/SearchExecutionStateActions';
import SearchParameterStore from './SearchParameterStore';

const defaultExecutionState = Immutable.fromJS({
  parameter_bindings: {},
});

export default Reflux.createStore({
  listenables: [SearchExecutionStateActions],

  executionState: defaultExecutionState,

  init() {
    this.listenTo(SearchParameterStore, this.handleSearchParameterChange, this.handleSearchParameterChange);
  },

  getInitialState() {
    return this.executionState;
  },

  handleSearchParameterChange(parameters) {
    const bindings = this.executionState.get('parameter_bindings');

    if (bindings) {
      // Cleanup the parameter bindings to only keep declared parameters
      const filteredBindings = bindings.filter((_, name) => parameters.has(name));
      this.executionState = this.executionState.set('parameter_bindings', filteredBindings);
      this.trigger(this.executionState);
    }
  },

  clear() {
    this.trigger(defaultExecutionState);
  },

  replace(bindings) {
    let parameterBindings = Immutable.Map();

    bindings.forEach((value, parameterName) => {
      parameterBindings = parameterBindings.set(parameterName, Immutable.fromJS({
        type: 'value',
        value: value,
      }));
    });

    this.executionState = this.executionState.set('parameter_bindings', parameterBindings);
    this.trigger(this.executionState);
  },

  setParameterValues(parameterMap) {
    parameterMap.forEach((value, parameterName) => {
      this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], Immutable.fromJS({
        type: 'value',
        value: value,
      }));
    });
    this.trigger(this.executionState);
  },

  bindParameterValue(parameterName, value) {
    this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], Immutable.fromJS({
      type: 'value',
      value: value,
    }));
    this.trigger(this.executionState);
  },
});
