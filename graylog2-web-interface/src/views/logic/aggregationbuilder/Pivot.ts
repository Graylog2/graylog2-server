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
import type { TimeUnits } from 'views/Constants';
import { DEFAULT_PIVOT_LIMIT } from 'views/Constants';

export type AutoTimeConfig = {
  type: 'auto',
  scaling: number,
};

export type TimeUnitConfig = {
  type: 'timeunit',
  value: number,
  unit: keyof typeof TimeUnits,
};

export type TimeConfigType = {
  interval: AutoTimeConfig | TimeUnitConfig,
};

export type ValuesConfigType = {
  limit: number,
};

export type PivotConfigType = TimeConfigType | ValuesConfigType;

export const DateType = 'time';
export const ValuesType = 'values';

export type FieldTypeCategory = typeof DateType | typeof ValuesType

export type PivotJson = {
  fields: Array<string>,
  type: string,
  config: PivotConfigType,
};

type InternalState = {
  fields: Array<string>,
  type: string,
  config: PivotConfigType,
};

export default class Pivot {
  _value: InternalState;

  constructor(fields: Array<string>, type: string, config: PivotConfigType = { limit: DEFAULT_PIVOT_LIMIT }) {
    this._value = { fields, type, config };
  }

  get fields() {
    return this._value.fields;
  }

  get type() {
    return this._value.type;
  }

  get config() {
    return this._value.config;
  }

  static create(fields: Array<string>, type: string, config: PivotConfigType = { limit: DEFAULT_PIVOT_LIMIT }) {
    return new Pivot(fields, type, config);
  }

  static fromJSON(value: PivotJson) {
    const { fields, type, config = { limit: DEFAULT_PIVOT_LIMIT } } = value;

    return new Pivot(fields, type, config);
  }

  toJSON(): PivotJson {
    const { fields, type, config } = this._value;

    return { fields, type, config };
  }
}
