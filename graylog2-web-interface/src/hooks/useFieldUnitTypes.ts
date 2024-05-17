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
import minBy from 'lodash/minBy';
import maxBy from 'lodash/maxBy';

import type { MetricUnitType } from 'views/types';
import type { SeriesUnitState } from 'views/logic/aggregationbuilder/SeriesUnit';

import supportedUnits from '../../../graylog-shared-resources/units/supported_units.json';

type UnitConversionAction = 'MULTIPLY' | 'DIVIDE'

const sourceUnits = supportedUnits.units as FieldUnitTypes;
export type UnitJson = {
  abbrev: string,
  name: string,
  unit_type: MetricUnitType,
  conversion?: {
    value: number,
    action: UnitConversionAction
  } | undefined
}
type FieldUnitTypes = Record<MetricUnitType, Array<UnitJson>>
type ConversionParams = SeriesUnitState;
type ConvertedResult = { value: number | null, unit: SeriesUnitState };

const _getBaseUnit = (units: FieldUnitTypes, unitType: MetricUnitType): UnitJson => units[unitType].find(({ conversion }) => !conversion);

const _convertValueToBaseUnit = (units: FieldUnitTypes, value: number, params: ConversionParams): ConvertedResult => {
  const unit = units[params.unitType].find(({ name }) => params.unit === name);
  const baseUnit = _getBaseUnit(units, params.unitType);
  if (baseUnit.name === params.unit) return ({ value, unit: { unitType: baseUnit.unit_type, unit: baseUnit.name } });
  const res: ConvertedResult = ({ value: null, unit: { unitType: baseUnit.unit_type, unit: baseUnit.name } });

  if (unit?.conversion?.action === 'MULTIPLY') {
    res.value = value * unit.conversion.value;
  }

  if (unit?.conversion?.action === 'DIVIDE') {
    res.value = value / unit.conversion.value;
  }

  return res;
};

const _convertValueToUnit = (units: FieldUnitTypes, value: number, fromParams: ConversionParams, toParams: ConversionParams): ConvertedResult => {
  if (fromParams.unitType === toParams.unitType && fromParams.unit === toParams.unit) return ({ value, unit: { unitType: fromParams.unitType, unit: fromParams.unit } });

  const baseValue = _convertValueToBaseUnit(units, value, fromParams);
  const unit = units[toParams.unitType].find(({ name }) => toParams.unit === name);
  const res: ConvertedResult = ({ value: null, unit: { unitType: unit.unit_type, unit: unit.name } });

  if (unit?.conversion?.action === 'MULTIPLY') {
    res.value = baseValue.value / unit.conversion.value;
  }

  if (unit?.conversion?.action === 'DIVIDE') {
    res.value = baseValue.value * unit.conversion.value;
  }

  return res;
};

const _getPrettifiedValue = (units: FieldUnitTypes, value: number, params: ConversionParams) => {
  const currentUnit = units?.[params?.unitType] ?? null;
  if (!(value && currentUnit)) return ({ value, unit: currentUnit });

  const allConvertedValues = Object.values(currentUnit).map((unit) => _convertValueToUnit(units, value, params, { unit: unit.name, unitType: unit.unit_type }));

  console.log({ allConvertedValues });
  const filtratedValues = allConvertedValues.filter(({ value: val }) => val >= 1);

  if (filtratedValues.length > 0) {
    return minBy(filtratedValues, ({ value: val }) => val);
  }

  const filtratedValuesLower = allConvertedValues.filter(({ value: val }) => val < 1);

  return maxBy(filtratedValuesLower, ({ value: val }) => val);
};

const useFieldUnitTypes = () => {
  const units = useMemo<FieldUnitTypes>(() => sourceUnits, []);
  const getBaseUnit = useCallback((fieldType: MetricUnitType) => _getBaseUnit(units, fieldType), [units]);
  const convertValueToBaseUnit = useCallback((value: number, params: ConversionParams) => _convertValueToBaseUnit(units, value, params), [units]);
  const convertValueToUnit = useCallback((value: number, fromParams: ConversionParams, toParams: ConversionParams) => _convertValueToUnit(units, value, fromParams, toParams), [units]);
  const getPrettifiedValue = useCallback((value: number, params: ConversionParams) => _getPrettifiedValue(units, value, params), [units]);

  return { units, getBaseUnit, convertValueToBaseUnit, convertValueToUnit, getPrettifiedValue };
};

export default useFieldUnitTypes;
