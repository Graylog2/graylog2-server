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

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';

import type { DirectionJson } from './Direction';
import Direction from './Direction';

export type SortConfigJson = {
  type: string,
  field: string,
  direction: DirectionJson,
};

type SortConfigType = 'pivot' | 'series';

type InternalState = {
  type: SortConfigType,
  field: string,
  direction: Direction,
};

export default class SortConfig {
  static PIVOT_TYPE = 'pivot' as const;

  static SERIES_TYPE = 'series' as const;

  private readonly _value: InternalState;

  constructor(type: SortConfigType, field: string, direction: Direction) {
    this._value = { type, field, direction };
  }

  get type() {
    return this._value.type;
  }

  get field() {
    return this._value.field;
  }

  get direction() {
    return this._value.direction;
  }

  toJSON(): SortConfigJson {
    const { type, field, direction } = this._value;

    return {
      type,
      field,
      direction: direction as unknown as DirectionJson,
    };
  }

  static fromJSON(value: SortConfigJson) {
    const { type, field, direction } = value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .type(type)
      .field(field)
      .direction(Direction.fromJSON(direction))
      .build();
  }

  static __registrations: { [key: string]: typeof SortConfig } = {};

  static registerSubtype(type: string, implementingClass: typeof SortConfig) {
    this.__registrations[type.toLocaleLowerCase()] = implementingClass;
  }

  static fromPivot(pivot: Pivot) {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .type(this.PIVOT_TYPE)
      .field(pivot.field)
      .direction(Direction.Ascending)
      .build();
  }

  static fromSeries(series: Series) {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder()
      .type(this.SERIES_TYPE)
      .field(series.function)
      .direction(Direction.Descending)
      .build();
  }

  // eslint-disable-next-line @typescript-eslint/no-use-before-define
  toBuilder(): Builder {
    const { type, field, direction } = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map({ type, field, direction }));
  }
}

type BuilderState = Immutable.Map<string, any>;
export class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  type(value: string) {
    return new Builder(this.value.set('type', value));
  }

  field(value: string) {
    return new Builder(this.value.set('field', value));
  }

  direction(value: Direction) {
    return new Builder(this.value.set('direction', value));
  }

  build() {
    const { type, field, direction } = this.value.toObject();

    return new SortConfig(type, field, direction);
  }
}
