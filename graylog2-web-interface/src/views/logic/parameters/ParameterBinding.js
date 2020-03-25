// @flow strict
import * as Immutable from 'immutable';

type InternalState = {
  type: string,
  value: string,
};

type InternalStateBuilder = Immutable.Map<string, any>;

export type ParameterBindingJsonRepresentation = {
  type: string,
  value: any,
};

export default class ParameterBinding {
  _value: InternalState;

  constructor(type: string, value: string) {
    this._value = { type, value };
  }

  get type(): string {
    return this._value.type;
  }

  get value(): string {
    return this._value.value;
  }

  // eslint-disable-next-line no-use-before-define
  toBuilder(): Builder {
    const { type, value } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Immutable.Map({ type, value }));
  }

  static create(type: string, value: any): ParameterBinding {
    return new ParameterBinding(type, value);
  }

  static forValue(value: any): ParameterBinding {
    return ParameterBinding.create('value', value);
  }

  static empty() {
    return ParameterBinding.create('value', '');
  }

  toJSON(): ParameterBindingJsonRepresentation {
    const { type, value } = this._value;

    return {
      type,
      value,
    };
  }

  static fromJSON(json: ?ParameterBindingJsonRepresentation): ?ParameterBinding {
    if (json == null) {
      return null;
    }
    const { type, value } = json;
    return ParameterBinding.create(type, value);
  }
}

class Builder {
  _value: InternalStateBuilder;

  constructor(value: InternalStateBuilder = Immutable.Map()) {
    this._value = value;
  }

  type(value: string): Builder {
    return new Builder(this._value.set('type', value));
  }

  value(value: string): Builder {
    return new Builder(this._value.set('value', value));
  }

  build(): ParameterBinding {
    const { type, value } = this._value.toObject();
    return new ParameterBinding(type, value);
  }
}
