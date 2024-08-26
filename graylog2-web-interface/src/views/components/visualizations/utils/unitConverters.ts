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

import minBy from 'lodash/minBy';
import maxBy from 'lodash/maxBy';
import mapValues from 'lodash/mapValues';
import get from 'lodash/get';
import keyBy from 'lodash/keyBy';
import isNumber from 'lodash/isNumber';
import toNumber from 'lodash/toNumber';

import type { FieldUnitType } from 'views/types';
import type { FieldUnitState } from 'views/logic/aggregationbuilder/FieldUnit';

import supportedUnits from '../../../../../../graylog2-server/src/main/resources/units/supported_units.json';

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
  useInPrettier: boolean,
  conversion?: {
    value: number,
    action: UnitConversionAction
  } | undefined
}
type FieldUnitTypesJson = Record<FieldUnitType, Array<UnitJson>>
type FieldUnitTypes = Record<FieldUnitType, Array<Unit>>
export type ConversionParams = FieldUnitState;
export type ConvertedResult = { value: number | null, unit: Unit };

const isUnitUsableInPrettier = (unitJson: UnitJson): boolean => !(unitJson.unit_type === 'percent' && unitJson.abbrev === 'd%');

const unitFromJson = (unitJson: UnitJson): Unit => ({
  type: unitJson.type,
  abbrev: unitJson.abbrev,
  name: unitJson.name,
  unitType: unitJson.unit_type,
  conversion: unitJson.conversion,
  useInPrettier: isUnitUsableInPrettier(unitJson),
});
export const mappedUnitsFromJSON: FieldUnitTypes = <FieldUnitTypes>mapValues(sourceUnits, (unitsJson: Array<UnitJson>):Array<Unit> => unitsJson.map((unitJson: UnitJson): Unit => unitFromJson(unitJson)));

export const _getBaseUnit = (units: FieldUnitTypes, unitType: FieldUnitType): Unit => units[unitType].find(({ type }) => type === 'base');

const _convertValueToBaseUnit = (units: FieldUnitTypes, value: number, params: ConversionParams): ConvertedResult => {
  if (!(isNumber(value) && params?.unitType && params?.abbrev)) return ({ value: null, unit: null });
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
  if (!(isNumber(value) && fromParams?.unitType && fromParams?.abbrev && toParams?.abbrev && toParams?.unitType)) return ({ value: null, unit: null });

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

export const _getPrettifiedValue = (units: FieldUnitTypes, initValue: number | string, params: ConversionParams): ConvertedResult => {
  const currentUnit = units?.[params?.unitType] ?? null;

  const value = initValue === null ? null : toNumber(initValue);
  if (!(value && currentUnit)) return ({ value, unit: currentUnit ? currentUnit.find(({ abbrev }) => abbrev === params.abbrev) : null });

  const allConvertedValues = Object.values(currentUnit).map((unit: Unit) => _convertValueToUnit(units, value, params, { abbrev: unit.abbrev, unitType: unit.unitType }));

  const filtratedValues = allConvertedValues.filter(({ value: val, unit }) => val >= 1 && unit.useInPrettier);

  if (filtratedValues.length > 0) {
    return minBy(filtratedValues, ({ value: val }) => val);
  }

  const filtratedValuesLower = allConvertedValues.filter(({ value: val, unit }) => val < 1 && unit.useInPrettier);

  return maxBy(filtratedValuesLower, ({ value: val }) => val);
};

export type ConvertValueToUnit = (value: number, fromParams: ConversionParams, toParams: ConversionParams) => ConvertedResult
export const convertValueToBaseUnit = (value: number, params: ConversionParams) => _convertValueToBaseUnit(mappedUnitsFromJSON, value, params);
export const convertValueToUnit: ConvertValueToUnit = (value, fromParams, toParams) => _convertValueToUnit(mappedUnitsFromJSON, value, fromParams, toParams);
export const getPrettifiedValue = (value: number, params: ConversionParams) => _getPrettifiedValue(mappedUnitsFromJSON, value, params);
export const getBaseUnit = (fieldType: FieldUnitType) => _getBaseUnit(mappedUnitsFromJSON, fieldType);
export const unitsByAbbrev = mapValues(mappedUnitsFromJSON, (list) => keyBy(list, 'abbrev'));
export const getUnitInfo = (unitType: FieldUnitType, abbrev: string) => get(unitsByAbbrev, [unitType, abbrev]);
