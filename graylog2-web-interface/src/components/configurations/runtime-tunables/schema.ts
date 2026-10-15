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

import type { Condition, TunableSchema } from './types';

export type FieldKind = 'boolean' | 'integer' | 'number' | 'string' | 'enum' | 'duration';

const isNullType = (schema: TunableSchema) => schema.type === 'null';

export const isNullable = (schema: TunableSchema) =>
  (Array.isArray(schema.type) && schema.type.includes('null')) || !!schema.anyOf?.some(isNullType);

export const nonNullSchema = (schema: TunableSchema): TunableSchema => {
  if (schema.anyOf) {
    const { anyOf, ...outer } = schema;
    const branch = anyOf.find((candidate) => !isNullType(candidate)) ?? {};

    return { ...branch, ...outer };
  }
  if (Array.isArray(schema.type)) {
    const types = schema.type.filter((type) => type !== 'null');

    return { ...schema, type: types.length === 1 ? types[0] : types };
  }

  return schema;
};

export const fieldKind = (schema: TunableSchema): FieldKind => {
  const actual = nonNullSchema(schema);
  if (actual.enum) {
    return 'enum';
  }
  switch (actual.type) {
    case 'boolean':
    case 'integer':
    case 'number':
      return actual.type;
    default:
      return actual.format === 'duration' ? 'duration' : 'string';
  }
};

export const orderedProperties = (schema: TunableSchema): Array<[string, TunableSchema]> =>
  Object.entries(schema.properties ?? {})
    .map((entry, index) => ({ entry, index }))
    .sort((a, b) => (a.entry[1]['x-graylog-order'] ?? 0) - (b.entry[1]['x-graylog-order'] ?? 0) || a.index - b.index)
    .map(({ entry }) => entry);

const asText = (value: unknown) => (value === null || value === undefined ? 'null' : String(value));

const conditionMatches = ({ property, in: allowed }: Condition, siblings: unknown) =>
  allowed.includes(asText((siblings as Record<string, unknown> | undefined)?.[property]));

export const conditionsMatch = (conditions: Array<Condition> | undefined, siblings: unknown) =>
  (conditions ?? []).every((condition) => conditionMatches(condition, siblings));

export const isVisible = (schema: TunableSchema) => !schema['x-graylog-hidden'];

export const isActive = (schema: TunableSchema, siblings: unknown) =>
  isVisible(schema) && conditionsMatch(schema['x-graylog-enabled-when'], siblings);
