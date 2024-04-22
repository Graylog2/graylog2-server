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

type UnitType = 'file_size' | 'time' | 'percentage'

type FieldUnitTypes = Record<UnitType, { id: string, name: string, options: Array<{ id: string, name: string}> }>

const useFieldUnitTypes = () => useMemo<FieldUnitTypes>(() => ({
  file_size: { id: 'file_size', name: 'File size', options: fileSizes },
  time: { id: 'time', name: 'Time', options: times },
  percentage: { id: 'percentage', name: 'Percentage', options: [{ id: 'p', name: '%' }] },
}), []);

export default useFieldUnitTypes;
