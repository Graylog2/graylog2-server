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
import mapValues from 'lodash/mapValues';

import type { FieldUnitJson } from 'views/logic/aggregationbuilder/FieldUnit';
import FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import type { FieldUnitsFormValues } from 'views/types';

type FieldName = string;
export type UnitsConfigJson = Record<FieldName, FieldUnitJson>

type InternalState = Record<FieldName, FieldUnit>

export default class UnitsConfig {
  private readonly _value: InternalState;

  constructor(units: InternalState) {
    this._value = units;
  }

  getFieldUnit(fieldName: string) {
    return this._value[fieldName];
  }

  toJSON(): UnitsConfigJson {
    const units: InternalState = this._value;

    return <UnitsConfigJson>mapValues(units, (unit: FieldUnit) => unit.toJSON());
  }

  toFormValues(): FieldUnitsFormValues {
    const units = this._value;

    return mapValues(units, (unit: FieldUnit) => ({ unitType: unit.unitType, abbrev: unit.abbrev }));
  }

  static fromJSON(value: UnitsConfigJson) {
    const unitsJson = value;
    const units = mapValues(unitsJson, (unit: FieldUnitJson) => FieldUnit.fromJSON(unit));

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(units)).build();
  }

  static empty() {
    return new UnitsConfig({});
  }

  toBuilder(): Builder {
    const units = this._value;

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return new Builder(Immutable.Map(units));
  }
}

type BuilderState = Immutable.Map<string, FieldUnit>;
export class Builder {
  private readonly value: BuilderState;

  constructor(value: BuilderState = Immutable.Map()) {
    this.value = value;
  }

  static create(values: InternalState) {
    return new Builder(Immutable.Map(values));
  }

  setFieldUnit(fieldName: FieldName, value: FieldUnit) {
    return new Builder(this.value.set(fieldName, value));
  }

  merge(values: InternalState) {
    return new Builder(this.value.merge(values));
  }

  build() {
    const units = this.value.toObject();

    return new UnitsConfig(units);
  }
}
