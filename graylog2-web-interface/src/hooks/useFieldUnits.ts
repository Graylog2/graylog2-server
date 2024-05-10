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

type UnitType = 'file_size' | 'time' | 'percentage'

type FieldUnits = Record<string, { unit_type: UnitType, unit: string }>

const useFieldUnits = () => useMemo<FieldUnits>(() => ({
  http_response_code: { unit_type: 'file_size', unit: 'bytes' },
}), []);

export default useFieldUnits;
