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

import type { QueryValue } from 'views/logic/valueactions/ValueActionQueryHelper';

type Row = { [key: string]: unknown };

// Identifies a row by the values of its row grouping fields, so the bulk actions can read the values from the selected keys.
// Including the field names prevents a selection from carrying over to a different grouping with the same values.
export const rowSelectionKey = (row: Row, rowFieldNames: Array<string>) =>
  JSON.stringify(rowFieldNames.map((field) => [field, row[field]]));

export const selectedFieldValues = (selectedKeys: Array<string>, field: string): Array<QueryValue> =>
  uniq(selectedKeys.map((key) => Object.fromEntries(JSON.parse(key))[field]));
