import Reflux from 'reflux';
import Immutable from 'immutable';

import SearchExecutionStateActions from 'enterprise/actions/SearchExecutionStateActions';
import SearchParameterStore from './SearchParameterStore';

const defaultExecutionState = Immutable.fromJS({
  parameter_bindings: {},
});

const newParameterBindingValue = (value) => Immutable.fromJS({
  type: 'value',
  value: value,
});

const setParameterBindings = (executionState, bindings) => {
  return executionState.set('parameter_bindings', bindings);
};

const getParameterBindings = executionState => executionState.get('parameter_bindings');

const getParameterBindingsAsMap = bindings => bindings.flatMap((value, name) => ({ [name]: value.get('value') }));

export { newParameterBindingValue, setParameterBindings, getParameterBindings, getParameterBindingsAsMap };

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

  replace(executionState, trigger = true) {
    this.executionState = executionState;
    if (trigger) {
      this.trigger(this.executionState);
    }
  },

  setParameterValues(parameterMap) {
    parameterMap.forEach((value, parameterName) => {
      this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], newParameterBindingValue(value));
    });
    this.trigger(this.executionState);
    return this.executionState;
  },

  bindParameterValue(parameterName, value) {
    this.executionState = this.executionState.setIn(['parameter_bindings', parameterName], newParameterBindingValue(value));
    this.trigger(this.executionState);
  },
});
