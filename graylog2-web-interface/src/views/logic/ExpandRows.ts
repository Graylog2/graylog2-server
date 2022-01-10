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
import { flatten, setWith } from 'lodash';

import type { ColLeaf, Leaf, NonLeaf, RowInner, RowLeaf } from 'views/logic/searchtypes/pivot/PivotHandler';

type ResultEntry = Result | string | number;

type Result = { [key: string]: ResultEntry };
type Results = Array<Result>;

const expandRows = (fieldNames: Array<string>, columnFieldNames: Array<string>, rows: Array<Leaf | NonLeaf>): Results => {
  if (!rows) {
    return [];
  }

  const expanded = [];

  rows.forEach((row: Leaf | NonLeaf) => {
    const { values } = row;
    const result = {};

    row.key.forEach((key, idx) => {
      result[fieldNames[idx]] = key;
    });

    (values as Array<ColLeaf | RowLeaf | RowInner>).forEach(({ key, value }) => {
      const translatedKeys = flatten(key.map((k, idx) => (idx < key.length - 1 && columnFieldNames[idx] ? [columnFieldNames[idx], k] : k)));

      setWith(result, translatedKeys, value, Object);
    });

    expanded.push(result);
  });

  return expanded;
};

export default expandRows;
