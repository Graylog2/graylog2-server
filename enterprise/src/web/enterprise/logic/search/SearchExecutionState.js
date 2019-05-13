// @flow strict
import * as Immutable from 'immutable';
import ParameterBinding from '../parameters/ParameterBinding';

export type ParameterBindings = Immutable.Map<string, ParameterBinding>;

type InternalState = {
  parameterBindings: ParameterBindings,
};

type JsonRepresentation = {
  parameter_bindings: ParameterBindings;
};

export default class SearchExecutionState {
  _value: InternalState;

  constructor(parameterBindings: ParameterBindings = Immutable.Map()) {
    this._value = { parameterBindings };
  }

  get parameterBindings(): ParameterBindings {
    return this._value.parameterBindings;
  }

  toBuilder(): Builder {
    const { parameterBindings } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ parameterBindings }));
  }

  static create(parameterBindings: ParameterBindings): SearchExecutionState {
    return new SearchExecutionState(parameterBindings);
  }

  static empty(): SearchExecutionState {
    return new SearchExecutionState();
  }

  toJSON(): JsonRepresentation {
    const { parameterBindings } = this._value;

    return {
      parameter_bindings: parameterBindings,
    };
  }

  static fromJSON(value: JsonRepresentation): SearchExecutionState {
    // eslint-disable-next-line camelcase
    const { parameter_bindings } = value;
    return SearchExecutionState.create(parameter_bindings);
  }
}

type InternalBuilderState = Immutable.Map<string, any>;

class Builder {
  value: InternalBuilderState;

  constructor(value: Immutable.Map = Immutable.Map()) {
    this.value = value;
  }

  parameterBindings(newBindings: ParameterBindings): Builder {
    return new Builder(this.value.set('parameterBindings', newBindings));
  }

  build(): SearchExecutionState {
    const { parameterBindings } = this.value.toObject();
    return new SearchExecutionState(parameterBindings);
  }
}

const getParameterBindingValue = (executionState: SearchExecutionState, parameterName: string) => executionState.parameterBindings.get(parameterName, ParameterBinding.empty()).value;

const getParameterBindingsAsMap = (bindings: ParameterBindings): any => bindings.flatMap((binding: ParameterBinding, name: string) => ({ [name]: binding.value }));

export { getParameterBindingsAsMap, getParameterBindingValue };
