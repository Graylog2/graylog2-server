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

const fileSizes = [
  { id: 'bytes', name: 'B' },
  { id: 'k_bytes', name: 'KB' },
  { id: 'm_bytes', name: 'MB' },
  { id: 'g_bytes', name: 'GB' },
  { id: 't_bytes', name: 'TB' },
];

const times = [
  { id: 'ms', name: 'MS' },
  { id: 's', name: 'Seconds' },
  { id: 'min', name: 'Minutes' },
  { id: 'h', name: 'Hours' },
  { id: 'd', name: 'Days' },
  { id: 'mon', name: 'Months' },
  { id: 'y', name: 'years' },
];

type UnitConversionAction = 'MULTIPLY' | 'DIVIDE'

export type Unit = {
  abbrev: string,
  name: string,
  unit_type: MetricUnitType,
  conversion?: {
    value: number,
    action: UnitConversionAction
  }
}
type FieldUnitTypes = Record<MetricUnitType, Array<Unit>>

const useFieldUnitTypes = () => useMemo<FieldUnitTypes>(() => ({
  size: [
    {
      abbrev: 'b',
      name: 'byte',
      unit_type: 'size',
    },
    {
      abbrev: 'kb',
      name: 'kilobyte',
      unit_type: 'size',
      conversion: {
        value: 1000,
        action: 'MULTIPLY',
      },
    },
    {
      abbrev: 'Mb',
      name: 'megabyte',
      unit_type: 'size',
      conversion: {
        value: 1000000,
        action: 'MULTIPLY',
      },
    },
    {
      abbrev: 'Gb',
      name: 'gigabyte',
      unit_type: 'size',
      conversion: {
        value: 1000000000,
        action: 'MULTIPLY',
      },
    },
  ],
  time: [
    {
      abbrev: 'ns',
      name: 'nanosecond',
      unit_type: 'time',
      conversion: {
        value: 1000000000,
        action: 'DIVIDE',
      },
    },
    {
      abbrev: 'μs',
      name: 'microsecond',
      unit_type: 'time',
      conversion: {
        value: 1000000,
        action: 'DIVIDE',
      },
    },
    {
      abbrev: 'ms',
      name: 'millisecond',
      unit_type: 'time',
      conversion: {
        value: 1000,
        action: 'DIVIDE',
      },
    },
    {
      abbrev: 's',
      name: 'second',
      unit_type: 'time',
    },
    {
      abbrev: 'min',
      name: 'minute',
      unit_type: 'time',
      conversion: {
        value: 60,
        action: 'MULTIPLY',
      },
    },
    {
      abbrev: 'h',
      name: 'hour',
      unit_type: 'time',
      conversion: {
        value: 3600,
        action: 'MULTIPLY',
      },
    },
    {
      abbrev: 'd',
      name: 'day',
      unit_type: 'time',
      conversion: {
        value: 86400,
        action: 'MULTIPLY',
      },
    },
    {
      abbrev: 'm',
      name: 'month',
      unit_type: 'time',
      conversion: {
        value: 2592000,
        action: 'MULTIPLY',
      },
    },
  ],
  percent: [
    {
      abbrev: '%',
      name: 'percent',
      unit_type: 'percent',
    },
  ],
}), []);

export default useFieldUnitTypes;
