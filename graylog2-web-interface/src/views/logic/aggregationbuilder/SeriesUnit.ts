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

import type { MetricUnitType } from 'views/types';

export type SeriesUnitJson = {
  abbrev: string,
  unit_type: MetricUnitType,
};

export type SeriesUnitState = {
  abbrev: string,
  unitType: MetricUnitType,
};

export default class SeriesUnit {
  private readonly _value: SeriesUnitState;

  constructor(unitType: MetricUnitType, abbrev: string) {
    this._value = { unitType, abbrev };
  }

  get unitType() {
    return this._value.unitType;
  }

  get abbrev() {
    return this._value.abbrev;
  }

  get isDefined() {
    return this._value.abbrev && this._value.unitType;
  }

  toJSON() {
    const { abbrev, unitType } = this._value;

    return { abbrev, unit_type: unitType };
  }

  static fromJSON(value: SeriesUnitJson) {
    return new SeriesUnit(value?.unit_type, value?.abbrev);
  }

  static empty() {
    return new SeriesUnit(null, null);
  }

  toBuilder() {
    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(this._value));
  }
}

type BuilderState = Immutable.Map<string, any>;

class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  abbrev(newUnit: string) {
    return new Builder(this.value.set('abbrev', newUnit));
  }

  unitType(newUnitType: MetricUnitType) {
    return new Builder(this.value.set('unitType', newUnitType));
  }

  build() {
    const { abbrev, unitType } = this.value.toObject();

    return new SeriesUnit(unitType, abbrev);
  }
}
