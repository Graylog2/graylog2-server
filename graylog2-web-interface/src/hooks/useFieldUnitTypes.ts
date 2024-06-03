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
import omit from 'lodash/omit';
import set from 'lodash/set';
import mapValues from 'lodash/mapValues';
import get from 'lodash/get';
import keyBy from 'lodash/keyBy';

import type { FieldUnitType } from 'views/types';
import type { FieldUnitState } from 'views/logic/aggregationbuilder/FieldUnit';

import supportedUnits from '../../../graylog2-server/src/main/resources/units/supported_units.json';

type UnitConversionAction = 'MULTIPLY' | 'DIVIDE'

const sourceUnits = supportedUnits.units as FieldUnitTypesJson;
export type UnitJson = {
  type: 'base' | 'derived',
  abbrev: string,
  name: string,
  unit_type: FieldUnitType,
  conversion?: {
    value: number,
    action: UnitConversionAction
  } | undefined
}
export type Unit = {
  type: 'base' | 'derived',
  abbrev: string,
  name: string,
  unitType: FieldUnitType,
  conversion?: {
    value: number,
    action: UnitConversionAction
  } | undefined
}
type FieldUnitTypesJson = Record<FieldUnitType, Array<UnitJson>>
type FieldUnitTypes = Record<FieldUnitType, Array<Unit>>
export type ConversionParams = FieldUnitState;
export type ConvertedResult = { value: number | null, unit: Unit };

const unitFromJson = (unitJson: UnitJson): Unit => set<Unit>(omit(unitJson, 'unit_type'), 'unitType', unitJson.unit_type);

const _getBaseUnit = (units: FieldUnitTypes, unitType: FieldUnitType): Unit => units[unitType].find(({ type }) => type === 'base');

const _convertValueToBaseUnit = (units: FieldUnitTypes, value: number, params: ConversionParams): ConvertedResult => {
  const unit = units[params.unitType].find(({ abbrev }) => params.abbrev === abbrev);
  const baseUnit = _getBaseUnit(units, params.unitType);
  const res: ConvertedResult = ({
    value: null,
    unit: baseUnit,
  });

  if (baseUnit.abbrev === params.abbrev) {
    res.value = value;

    return res;
  }

  if (unit?.conversion?.action === 'MULTIPLY') {
    res.value = value * unit.conversion.value;
  }

  if (unit?.conversion?.action === 'DIVIDE') {
    res.value = value / unit.conversion.value;
  }

  return res;
};

const _convertValueToUnit = (units: FieldUnitTypes, value: number, fromParams: ConversionParams, toParams: ConversionParams): ConvertedResult => {
  if (fromParams.unitType === toParams.unitType && fromParams.abbrev === toParams.abbrev) {
    const unit = units[toParams.unitType].find(({ abbrev }) => toParams.abbrev === abbrev);

    return ({ value, unit });
  }

  const baseValue = _convertValueToBaseUnit(units, value, fromParams);
  const unit = units[toParams.unitType].find(({ abbrev }) => toParams.abbrev === abbrev);
  const res: ConvertedResult = ({ value: null, unit });

  if (baseValue.unit.abbrev === toParams.abbrev) {
    res.value = baseValue.value;

    return res;
  }

  if (unit?.conversion?.action === 'MULTIPLY') {
    res.value = baseValue.value / unit.conversion.value;
  }

  if (unit?.conversion?.action === 'DIVIDE') {
    res.value = baseValue.value * unit.conversion.value;
  }

  return res;
};

const _getPrettifiedValue = (units: FieldUnitTypes, value: number, params: ConversionParams): ConvertedResult => {
  const currentUnit = units?.[params?.unitType] ?? null;
  if (!(value && currentUnit)) return ({ value, unit: currentUnit ? currentUnit.find(({ abbrev }) => abbrev === params.abbrev) : null });

  const allConvertedValues = Object.values(currentUnit).map((unit) => _convertValueToUnit(units, value, params, { abbrev: unit.abbrev, unitType: unit.unitType }));

  const filtratedValues = allConvertedValues.filter(({ value: val }) => val >= 1);

  if (filtratedValues.length > 0) {
    return minBy(filtratedValues, ({ value: val }) => val);
  }

  const filtratedValuesLower = allConvertedValues.filter(({ value: val }) => val < 1);

  return maxBy(filtratedValuesLower, ({ value: val }) => val);
};

export type ConvertValueToUnit = (value: number, fromParams: ConversionParams, toParams: ConversionParams) => ConvertedResult

const useFieldUnitTypes = () => {
  const units = useMemo<FieldUnitTypes>(() => mapValues(sourceUnits, (unitsJson: Array<UnitJson>):Array<Unit> => unitsJson.map((unitJson) => unitFromJson(unitJson))), []);
  const getBaseUnit = useCallback((fieldType: FieldUnitType) => _getBaseUnit(units, fieldType), [units]);
  const convertValueToBaseUnit = useCallback((value: number, params: ConversionParams) => _convertValueToBaseUnit(units, value, params), [units]);
  const convertValueToUnit: ConvertValueToUnit = useCallback((value, fromParams, toParams) => _convertValueToUnit(units, value, fromParams, toParams), [units]);
  const getPrettifiedValue = useCallback((value: number, params: ConversionParams) => _getPrettifiedValue(units, value, params), [units]);
  const unitsByAbbrev = useMemo(() => mapValues(units, (list) => keyBy(list, 'abbrev')), [units]);
  const getUnitInfo = useCallback((unitType: FieldUnitType, abbrev: string) => get(unitsByAbbrev, [unitType, abbrev]), [unitsByAbbrev]);

  return { units, unitsByAbbrev, getUnitInfo, getBaseUnit, convertValueToBaseUnit, convertValueToUnit, getPrettifiedValue };
};

export default useFieldUnitTypes;
