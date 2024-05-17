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

import { useCallback, useMemo } from 'react';

import type { MetricUnitType } from 'views/types';

import supportedUnits from '../../../graylog-shared-resources/units/supported_units.json';

type UnitConversionAction = 'MULTIPLY' | 'DIVIDE'

const sourceUnits = supportedUnits.units as FieldUnitTypes;
export type Unit = {
  abbrev: string,
  name: string,
  unit_type: MetricUnitType,
  conversion?: {
    value: number,
    action: UnitConversionAction
  } | undefined
}
type FieldUnitTypes = Record<MetricUnitType, Array<Unit>>
type ConvertParams = { unitType: MetricUnitType, unitName: string}
type ConvertedResult = { value: number | null, unit: { unitType: MetricUnitType, unit: string }}

const _getBaseUnit = (units: FieldUnitTypes, unitType: MetricUnitType): Unit => units[unitType].find(({ conversion }) => !conversion);

const _convertValueToBaseUnit = (units: FieldUnitTypes, value: number, params: ConvertParams): ConvertedResult => {
  const unit = units[params.unitType].find(({ name }) => params.unitName === name);
  const baseUnit = _getBaseUnit(units, params.unitType);
  const res: ConvertedResult = ({ value: null, unit: { unitType: baseUnit.unit_type, unit: baseUnit.name } });

  if (unit.conversion.action === 'MULTIPLY') {
    res.value = value * unit.conversion.value;
  }

  if (unit.conversion.action === 'DIVIDE') {
    res.value = value / unit.conversion.value;
  }

  return res;
};

const _convertValueToUnit = (units: FieldUnitTypes, value: number, fromParams: ConvertParams, toParams: ConvertParams): ConvertedResult => {
  const baseValue = _convertValueToBaseUnit(units, value, fromParams);
  const unit = units[toParams.unitType].find(({ name }) => toParams.unitName === name);
  const res: ConvertedResult = ({ value: null, unit: { unitType: unit.unit_type, unit: unit.name } });

  if (unit.conversion.action === 'MULTIPLY') {
    res.value = baseValue.value / unit.conversion.value;
  }

  if (unit.conversion.action === 'DIVIDE') {
    res.value = baseValue.value * unit.conversion.value;
  }

  return res;
};

const useFieldUnitTypes = () => {
  const units = useMemo<FieldUnitTypes>(() => sourceUnits, []);
  const getBaseUnit = useCallback((fieldType: MetricUnitType) => _getBaseUnit(units, fieldType), [units]);
  const convertValueToBaseUnit = useCallback((value: number, params: ConvertParams) => _convertValueToBaseUnit(units, value, params), [units]);
  const convertValueToUnit = useCallback((value: number, fromParams: ConvertParams, toParams: ConvertParams) => _convertValueToUnit(units, value, fromParams, toParams), [units]);

  return { units, getBaseUnit, convertValueToBaseUnit, convertValueToUnit };
};

export default useFieldUnitTypes;
