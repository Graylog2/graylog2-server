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

import type { TunableSchema } from './types';
import validateTunable from './validateTunable';

const schema: TunableSchema = {
  type: 'object',
  properties: {
    enabled: { type: 'boolean' },
    path: { type: 'string', minLength: 1, 'x-graylog-enabled-when': [{ property: 'enabled', in: ['true'] }] },
    limit: { type: 'integer', minimum: 1, maximum: 10 },
    ratio: { type: ['number', 'null'] },
    mode: { type: 'string', enum: ['A', 'B'] },
    grace: { type: 'string', format: 'duration' },
  },
};

const valid = { enabled: true, path: '/x', limit: 5, ratio: null, mode: 'A', grace: 'PT1M' };

describe('validateTunable', () => {
  it('accepts valid values', () => {
    expect(validateTunable(schema, valid)).toEqual({});
  });

  it('reports errors by property', () => {
    expect(
      validateTunable(schema, {
        ...valid,
        path: '',
        limit: 11,
        mode: 'C',
        grace: 'soon',
      }),
    ).toEqual({
      path: 'Must not be empty',
      limit: 'Must be at most 10',
      mode: 'Select one of the options',
      grace: 'Enter a valid duration',
    });
  });

  it('skips inactive fields', () => {
    expect(validateTunable(schema, { ...valid, enabled: false, path: '' })).toEqual({});
  });

  it('requires non-nullable values', () => {
    expect(validateTunable(schema, { ...valid, limit: '' })).toEqual({ limit: 'Must be a number' });
  });
});
