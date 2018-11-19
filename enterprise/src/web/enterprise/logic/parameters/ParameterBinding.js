// @flow
import * as Immutable from 'immutable';

type InternalState = {
  type: string,
  value: string,
};

type InternalStateBuilder = Immutable.Map<string, any>;

type JsonRepresentation = {
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

  toJSON(): JsonRepresentation {
    const { type, value } = this._value;

    return {
      type,
      value,
    };
  }

  static fromJSON(json: JsonRepresentation): ParameterBinding {
    const { type, value } = json;
    return ParameterBinding.create(type, value);
  }
}

class Builder {
  value: InternalStateBuilder;

  constructor(value: Immutable.Map<string, *> = Immutable.Map()) {
    this.value = value;
  }

  type(value): Builder {
    return new Builder(this.value.set('type', value));
  }

  value(value): Builder {
    return new Builder(this.value.set('value', value));
  }

  build(): ParameterBinding {
    const { type, value } = this.value.toObject();
    return new ParameterBinding(type, value);
  }
}
