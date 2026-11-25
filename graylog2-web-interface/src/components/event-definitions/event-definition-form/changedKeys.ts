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
import isEqual from 'lodash/isEqual';
import isPlainObject from 'lodash/isPlainObject';

type Values = Record<string, unknown>;

// Server validation errors use flat keys, also for fields of nested configs, so nested changes are listed too.
const changedKeys = (previous: unknown, next: unknown): Array<string> => {
  if (!isPlainObject(previous) || !isPlainObject(next)) {
    return [];
  }

  const previousValues = previous as Values;
  const nextValues = next as Values;

  return [...new Set([...Object.keys(previousValues), ...Object.keys(nextValues)])]
    .filter((key) => !isEqual(previousValues[key], nextValues[key]))
    .flatMap((key) => [key, ...changedKeys(previousValues[key], nextValues[key])]);
};

export default changedKeys;
