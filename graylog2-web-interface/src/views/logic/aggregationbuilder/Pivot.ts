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

type ConfigType = { [key: string]: unknown };

export const DateType = 'time';
export const ValuesType = 'values';

export type PivotJson = {
  field: string,
  type: string,
  config: ConfigType,
};

type InternalState = {
  field: string,
  type: string,
  config: ConfigType,
};

export default class Pivot {
  _value: InternalState;

  constructor(field: string, type: string, config: ConfigType = {}) {
    this._value = { field, type, config };
  }

  get field() {
    return this._value.field;
  }

  get type() {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  static create(field: string, type: string, config: ConfigType = {}) {
    return new Pivot(field, type, config);
  }

  static fromJSON(value: PivotJson) {
    const { field, type, config = {} } = value;

    return new Pivot(field, type, config);
  }

  toJSON(): PivotJson {
    const { field, type, config } = this._value;

    return { field, type, config };
  }
}
