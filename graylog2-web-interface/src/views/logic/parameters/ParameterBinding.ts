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

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
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

  static fromJSON(json: ParameterBindingJsonRepresentation | undefined | null): ParameterBinding | undefined | null {
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
