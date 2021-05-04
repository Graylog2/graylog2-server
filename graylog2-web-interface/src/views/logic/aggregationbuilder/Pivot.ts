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
import { TimeUnits } from 'views/Constants';

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
}

export type PivotConfigType = TimeConfigType | ValuesConfigType;

export const DateType = 'time';
export const ValuesType = 'values';

export type PivotJson = {
  field: string,
  type: string,
  config: PivotConfigType,
};

type InternalState = {
  field: string,
  type: string,
  config: PivotConfigType,
};

export default class Pivot {
  _value: InternalState;

  constructor(field: string, type: string, config: PivotConfigType = { limit: 15 }) {
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

  static create(field: string, type: string, config: PivotConfigType = { limit: 15 }) {
    return new Pivot(field, type, config);
  }

  static fromJSON(value: PivotJson) {
    const { field, type, config = { limit: 15 } } = value;

    return new Pivot(field, type, config);
  }

  toJSON(): PivotJson {
    const { field, type, config } = this._value;

    return { field, type, config };
  }
}
