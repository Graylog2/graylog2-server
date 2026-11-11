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
import uniq from 'lodash/uniq';

import { isFunction } from 'views/logic/aggregationbuilder/Series';
import fieldTypeFor from 'views/logic/fieldtypes/FieldTypeFor';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import type { QueryValue } from 'views/logic/valueactions/ValueActionQueryHelper';

export type Row = { [key: string]: unknown };
export type FieldValues = { field: string; type: FieldType; values: Array<QueryValue> };

// Fields of the row grouping, which can be used to filter the query by the values of the selected rows.
export const selectableFieldsFor = (rowFieldNames: Array<string>, types: FieldTypeMappingsList, interactive: boolean) =>
  interactive ? rowFieldNames.filter((field) => !isFunction(field) && !fieldTypeFor(field, types).isDecorated()) : [];

const queryValueFor = (value: unknown): QueryValue | undefined =>
  typeof value === 'string' || typeof value === 'number' ? value : undefined;

// Identifies a row by the fields and values of all row grouping fields,
// so a selection does not carry over to a different grouping with the same values.
export const rowSelectionKey = (row: Row, rowFieldNames: Array<string>) =>
  JSON.stringify(rowFieldNames.map((field) => [field, row[field]]));

const parseRowSelectionKey = (key: string): Array<[string, unknown]> => JSON.parse(key);

export const rowSelectionLabel = (key: string) =>
  `Select ${parseRowSelectionKey(key)
    .map(([field, value]) => `${field} ${value}`)
    .join(', ')}`;

// Returns the distinct values of each selectable field for the selected rows.
export const selectedValuesByField = (
  selectedKeys: Array<string>,
  selectableFields: Array<string>,
  types: FieldTypeMappingsList,
): Array<FieldValues> => {
  const selectedRows = selectedKeys.map((key) => Object.fromEntries(parseRowSelectionKey(key)));

  return selectableFields.map((field) => ({
    field,
    type: fieldTypeFor(field, types),
    values: uniq(selectedRows.map((row) => queryValueFor(row[field])).filter((value) => value !== undefined)),
  }));
};
