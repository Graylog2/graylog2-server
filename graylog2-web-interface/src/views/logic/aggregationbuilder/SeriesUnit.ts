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
  unit: string,
  unit_type: MetricUnitType,
};

type InternalState = {
  unit: string,
  unitType: MetricUnitType,
};

export default class SeriesUnit {
  private readonly _value: InternalState;

  constructor(unitType: MetricUnitType, unit: string) {
    this._value = { unitType, unit };
  }

  get unitType() {
    return this._value.unitType;
  }

  get unit() {
    return this._value.unit;
  }

  toJSON() {
    const { unit, unitType } = this._value;

    return { unit, unit_type: unitType };
  }

  static fromJSON(value: SeriesUnitJson) {
    return new SeriesUnit(value?.unit_type, value?.unit);
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

  unit(newUnit: string) {
    return new Builder(this.value.set('unit', newUnit));
  }

  unitType(newUnitType: MetricUnitType) {
    return new Builder(this.value.set('unitType', newUnitType));
  }

  build() {
    const { unit, unitType } = this.value.toObject();

    return new SeriesUnit(unitType, unit);
  }
}
