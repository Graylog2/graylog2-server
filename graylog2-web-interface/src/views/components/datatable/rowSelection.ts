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

import { createCompositeKey, splitCompositeKey } from 'views/logic/compositeKey';

type Row = { [key: string]: unknown };

// Identifies a row by the values of its row grouping fields, so the bulk actions can read the values from the selected keys.
export const rowSelectionKey = (row: Row, rowFieldNames: Array<string>) =>
  createCompositeKey(rowFieldNames.map((field) => row[field]));

export const selectedFieldValues = (selectedKeys: Array<string>, rowFieldNames: Array<string>, field: string) => {
  const fieldIndex = rowFieldNames.indexOf(field);

  return uniq(selectedKeys.map((key) => splitCompositeKey(key)[fieldIndex]));
};
