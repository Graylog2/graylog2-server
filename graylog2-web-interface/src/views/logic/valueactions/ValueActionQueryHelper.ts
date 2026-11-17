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
import { MISSING_BUCKET_NAME } from 'views/Constants';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { concatQueryStrings, escape, formatTimestamp, predicate } from 'views/logic/queries/QueryHelper';

export type QueryValue = string | number;
export type QueryValueForClause = { value: QueryValue; type?: FieldType };

export const formatQueryValue = (value: QueryValue, type?: FieldType) => {
  if (value === '') {
    return '""';
  }

  return type?.type === 'date' ? formatTimestamp(value) : escape(value);
};

export const orValuesClause = (values: Array<QueryValueForClause>) =>
  `(${concatQueryStrings(
    values.map(({ value, type }) => formatQueryValue(value, type)),
    { operator: 'OR', withBrackets: false },
  )})`;

// The empty value bucket is matched with `NOT _exists_:field`, e.g. `((source:(a OR b)) OR (NOT _exists_:source))`.
// Each alternative is bracketed, because Lucene would otherwise treat `NOT` as prohibiting the field for the whole group.
export const fieldValueOrClause = (field: string, values: Array<QueryValueForClause>) => {
  const presentValues = values.filter(({ value }) => value !== MISSING_BUCKET_NAME);
  const clauses = [
    ...(presentValues.length > 0 ? [predicate(field, orValuesClause(presentValues))] : []),
    ...(presentValues.length < values.length ? [predicate(field, MISSING_BUCKET_NAME)] : []),
  ];

  return `(${concatQueryStrings(clauses, { operator: 'OR', withBrackets: true })})`;
};
