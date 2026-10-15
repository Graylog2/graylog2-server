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
import {
  conditionsMatch,
  fieldKind,
  isNullable,
  nonNullSchema,
  orderedProperties,
} from './schema';

describe('schema helpers', () => {
  it.each<[TunableSchema, string]>([
    [{ type: 'boolean' }, 'boolean'],
    [{ type: 'integer' }, 'integer'],
    [{ type: ['integer', 'null'] }, 'integer'],
    [{ type: 'string', enum: ['A', 'B'] }, 'enum'],
    [{ type: 'string', format: 'duration' }, 'duration'],
    [{ anyOf: [{ type: 'null' }, { type: 'string', enum: ['s3'] }], title: 'Cloud' }, 'enum'],
  ])('detects %j as %s', (schema, kind) => {
    expect(fieldKind(schema)).toBe(kind);
  });

  it('detects nullability and keeps outer attributes', () => {
    const schema: TunableSchema = {
      anyOf: [{ type: 'null' }, { type: 'integer' }],
      title: 'Limit',
      'x-graylog-order': 3,
    };

    expect(isNullable(schema)).toBe(true);
    expect(isNullable({ type: 'integer' })).toBe(false);
    expect(nonNullSchema(schema)).toEqual({ type: 'integer', title: 'Limit', 'x-graylog-order': 3 });
  });

  it('orders properties by x-graylog-order, keeping declaration order otherwise', () => {
    const schema: TunableSchema = {
      properties: { b: { 'x-graylog-order': 2 }, a: { 'x-graylog-order': 1 }, c: {} },
    };

    expect(orderedProperties(schema).map(([name]) => name)).toEqual(['c', 'a', 'b']);
  });

  it('matches conditions against sibling values as text', () => {
    expect(conditionsMatch([{ property: 'enabled', in: ['true'] }], { enabled: true })).toBe(true);
    expect(conditionsMatch([{ property: 'enabled', in: ['true'] }], { enabled: false })).toBe(false);
    expect(conditionsMatch([{ property: 'cloud', in: ['null'] }], { cloud: null })).toBe(true);
    expect(conditionsMatch(undefined, {})).toBe(true);
  });
});
