// @flow strict
import * as Immutable from 'immutable';
import GlobalOverride from './GlobalOverride';
import ParameterBinding from '../parameters/ParameterBinding';

export type ParameterBindings = Immutable.Map<string, ParameterBinding>;

type InternalState = {
  parameterBindings: ParameterBindings,
  globalOverride: ?GlobalOverride,
};

type JsonRepresentation = {
  global_override: ?GlobalOverride,
  parameter_bindings: ParameterBindings,
};

export default class SearchExecutionState {
  _value: InternalState;

  constructor(parameterBindings: ParameterBindings = Immutable.Map(), globalOverride: ?GlobalOverride) {
    this._value = { parameterBindings, globalOverride };
  }

  get parameterBindings(): ParameterBindings {
    return this._value.parameterBindings;
  }

  get globalOverride(): ?GlobalOverride {
    return this._value.globalOverride;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { globalOverride, parameterBindings } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ globalOverride, parameterBindings }));
  }

  static create(parameterBindings: ParameterBindings, globalOverride: ?GlobalOverride): SearchExecutionState {
    return new SearchExecutionState(parameterBindings, globalOverride);
  }

  static empty(): SearchExecutionState {
    return new SearchExecutionState();
  }

  toJSON(): JsonRepresentation {
    const { globalOverride, parameterBindings } = this._value;

    return {
      global_override: globalOverride,
      parameter_bindings: parameterBindings,
    };
  }

  static fromJSON(value: JsonRepresentation): SearchExecutionState {
    // eslint-disable-next-line camelcase
    const { global_override, parameter_bindings } = value;
    return SearchExecutionState.create(parameter_bindings, global_override);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  parameterBindings(newBindings: ParameterBindings): Builder {
    return new Builder(this.value.set('parameterBindings', newBindings));
  }

  globalOverride(globalOverride: GlobalOverride): Builder {
    return new Builder(this.value.set('globalOverride', globalOverride));
  }

  build(): SearchExecutionState {
    const { globalOverride, parameterBindings } = this.value.toObject();
    return new SearchExecutionState(parameterBindings, globalOverride);
  }
}

const getParameterBindingValue = (executionState: SearchExecutionState, parameterName: string) => executionState.parameterBindings.get(parameterName, ParameterBinding.empty()).value;

const getParameterBindingsAsMap = (bindings: ParameterBindings) => bindings.flatMap<string, string>((binding: ParameterBinding, name: string) => Immutable.Map({ [name]: binding.value }));

export { getParameterBindingsAsMap, getParameterBindingValue };
