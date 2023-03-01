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

import Parameter from 'views/logic/parameters/Parameter';

import type { ParameterJson } from './Parameter';

type InternalBuilderState = Immutable.Map<string, any>;

type InternalState = {
  lookupTable: string,
  key: string,
};

export type LookupTableParameterJson = ParameterJson & {
  lookup_table: string,
  key: string,
};

export default class LookupTableParameter extends Parameter {
  static type = 'lut-parameter-v1';

  _value2: InternalState;

  static Builder: typeof Builder;

  constructor(name: string, title: string, description: string, dataType: string, defaultValue: any, optional: boolean, lookupTable: string, key: string) {
    super(LookupTableParameter.type, name, title, description, dataType, defaultValue, optional);
    this._value2 = { lookupTable, key };
  }

  static create(_type: string, name: string, title: string, description: string, dataType: string, defaultValue: any,
    optional: boolean, lookupTable: string, key: string): LookupTableParameter {
    return new LookupTableParameter(name, title, description, dataType, defaultValue, optional, lookupTable, key);
  }

  toBuilder(): Builder {
    const { type, name, title, description, dataType, defaultValue, optional } = this._value;
    const { lookupTable, key } = this._value2;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({
      type,
      name,
      title,
      description,
      dataType,
      defaultValue,
      optional,
      lookupTable,
      key,
    }));
  }

  // screw you eslint, using param.constructor.needsBinding() is ugly
  // eslint-disable-next-line class-methods-use-this
  get needsBinding(): boolean {
    return false;
  }

  get lookupTable(): string {
    return this._value2.lookupTable;
  }

  get key(): string {
    return this._value2.key;
  }

  toJSON(): LookupTableParameterJson {
    const { type, name, title, description, dataType, defaultValue, optional } = this._value;
    const { lookupTable, key } = this._value2;

    return {
      type,
      name,
      title,
      description,
      data_type: dataType,
      default_value: defaultValue,
      optional,
      binding: undefined,
      lookup_table: lookupTable,
      key,
    };
  }

  // static fromJSON(json: LookupTableParameterJson): Parameter {
  static fromJSON(json: LookupTableParameterJson): LookupTableParameter {
    const { name, title, description, data_type, default_value, optional, lookup_table, key } = json;

    return new LookupTableParameter(name, title, description, data_type, default_value, optional, lookup_table, key);
  }

  static builder(): Builder {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .type(LookupTableParameter.type)
      .optional(false)
      .dataType('any');
  }
}

class Builder {
  value: InternalBuilderState;

  constructor(value: InternalBuilderState = Immutable.Map()) {
    this.value = value;
  }

  type(value: string): Builder {
    return new Builder(this.value.set('type', value));
  }

  name(value: string): Builder {
    return new Builder(this.value.set('name', value));
  }

  title(value: string): Builder {
    return new Builder(this.value.set('title', value));
  }

  description(value: string): Builder {
    return new Builder(this.value.set('description', value));
  }

  dataType(value: string): Builder {
    return new Builder(this.value.set('dataType', value));
  }

  defaultValue(value: any): Builder {
    return new Builder(this.value.set('defaultValue', value));
  }

  optional(value: boolean): Builder {
    return new Builder(this.value.set('optional', value));
  }

  lookupTable(value: string): Builder {
    return new Builder(this.value.set('lookupTable', value));
  }

  key(value: string): Builder {
    return new Builder(this.value.set('key', value));
  }

  build(): LookupTableParameter {
    const { name, title, description, dataType, defaultValue, optional, lookupTable, key } = this.value.toObject();

    return new LookupTableParameter(name, title, description, dataType, defaultValue, optional, lookupTable, key);
  }
}

LookupTableParameter.Builder = Builder;
