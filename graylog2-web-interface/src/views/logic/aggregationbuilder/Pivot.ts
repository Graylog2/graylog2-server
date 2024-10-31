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
  skip_empty_values: boolean,
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

const DEFAULT_PIVOT_CONFIG = { limit: DEFAULT_PIVOT_LIMIT, skip_empty_values: false };

export default class Pivot {
  _value: InternalState;

  constructor(fields: Array<string>, type: string, config: PivotConfigType = DEFAULT_PIVOT_CONFIG) {
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

  static create(fields: Array<string>, type: string, config: PivotConfigType = DEFAULT_PIVOT_CONFIG) {
    return new Pivot(fields, type, config);
  }

  static createValues(fields: Array<string>, config: ValuesConfigType = DEFAULT_PIVOT_CONFIG) {
    return Pivot.create(fields, ValuesType, config);
  }

  static fromJSON(value: PivotJson) {
    const { fields, type, config = DEFAULT_PIVOT_CONFIG } = value;

    return new Pivot(fields, type, config);
  }

  toJSON(): PivotJson {
    const { fields, type, config } = this._value;

    return { fields, type, config };
  }
}
