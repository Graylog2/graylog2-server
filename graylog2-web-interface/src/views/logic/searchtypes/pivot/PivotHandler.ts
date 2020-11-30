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
// @flow strict

export type Key = any;
export type Value = any;

type Keyed = {
  key: Array<Key>,
};

type SingleValue = {
  rollup: boolean,
  value: Value,
};

type MultiValue = {
  values: Array<Row>,
};

export type Leaf = { source: 'leaf' } & Keyed & MultiValue;

export type NonLeaf = { source: 'non-leaf' } & Keyed & MultiValue;

export type ColLeaf = { source: 'col-leaf' } & Keyed & SingleValue;

export type RowLeaf = { source: 'row-leaf' } & Keyed & SingleValue;

export type RowInner = { source: 'row-inner' } & Keyed & SingleValue;

export type Row = Leaf | NonLeaf | ColLeaf | RowLeaf | RowInner;
export type ResultId = string;
export type Rows = Array<Row>;
export type Result = {
  id: ResultId,
  rows: Rows,
  total: number,
  type: 'pivot',
};

export default {
  convert(result: Result) {
    return result;
  },
};
