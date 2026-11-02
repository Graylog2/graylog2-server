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

import NumberUtils from 'util/NumberUtils';

import type { TunableSchema } from './types';
import { enumLabel, fieldKind, nonNullSchema } from './schema';
import { DEFAULT_UNITS, splitDuration } from './duration';

const count = (amount: number, singular: string, plural: string) => `${amount} ${amount === 1 ? singular : plural}`;

const formatTunableValue = (schema: TunableSchema, value: unknown): string => {
  if (value === null || value === undefined) {
    return 'Not set';
  }
  const actual = nonNullSchema(schema);
  switch (fieldKind(actual)) {
    case 'boolean':
      return value ? 'Enabled' : 'Disabled';
    case 'enum':
      return enumLabel(actual, String(value));
    case 'integer':
    case 'number':
      return actual.format === 'bytes' ? NumberUtils.formatBytes(Number(value)) : String(value);
    case 'secret':
      return (value as { is_set?: boolean }).is_set ? 'Configured' : 'Not set';
    case 'duration': {
      const split = splitDuration(String(value), actual['x-graylog-units'] ?? DEFAULT_UNITS);

      return split ? count(split.value, split.unit.replace(/s$/, ''), split.unit) : String(value);
    }
    default:
      return String(value);
  }
};

export default formatTunableValue;
