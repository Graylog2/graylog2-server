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

import { useMemo } from 'react';

import type { MetricUnitType } from 'views/types';

import supportedUnits from '../../../graylog-shared-resources/units/supported_units.json';

type UnitConversionAction = 'MULTIPLY' | 'DIVIDE'

const units = supportedUnits.units as FieldUnitTypes;
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

const useFieldUnitTypes = () => useMemo<FieldUnitTypes>(() => units, []);

export default useFieldUnitTypes;
