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
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { concatQueryStrings, escape, formatTimestamp, predicate } from 'views/logic/queries/QueryHelper';

export type QueryValue = string | number;
export type QueryValueForClause = { value: QueryValue; type?: FieldType };

export const formatQueryValue = (value: QueryValue, type?: FieldType) =>
  type?.type === 'date' ? formatTimestamp(value) : escape(value);

export const orValuesClause = (values: Array<QueryValueForClause>) =>
  `(${concatQueryStrings(
    values.map(({ value, type }) => formatQueryValue(value, type)),
    { operator: 'OR', withBrackets: false },
  )})`;

export const fieldValueOrClause = (field: string, values: Array<QueryValueForClause>) =>
  `(${predicate(field, orValuesClause(values))})`;
