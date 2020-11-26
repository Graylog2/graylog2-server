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
import * as Immutable from 'immutable';

import GlobalOverride from './GlobalOverride';

import ParameterBinding from '../parameters/ParameterBinding';

export type ParameterBindings = Immutable.Map<string, ParameterBinding>;

type InternalState = {
  parameterBindings: ParameterBindings,
  globalOverride: GlobalOverride | undefined | null,
};

/* eslint-disable camelcase */
type JsonRepresentation = {
  global_override: GlobalOverride | undefined | null,
  parameter_bindings: ParameterBindings,
};
/* eslint-enable camelcase */

export default class SearchExecutionState {
  private _value: InternalState;

  constructor(parameterBindings: ParameterBindings = Immutable.Map(), globalOverride?: GlobalOverride) {
    this._value = { parameterBindings, globalOverride };
  }

  get parameterBindings(): ParameterBindings {
    return this._value.parameterBindings;
  }

  get globalOverride(): GlobalOverride | undefined | null {
    return this._value.globalOverride;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { globalOverride, parameterBindings } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ globalOverride, parameterBindings }));
  }

  static create(parameterBindings: ParameterBindings, globalOverride?: GlobalOverride): SearchExecutionState {
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
