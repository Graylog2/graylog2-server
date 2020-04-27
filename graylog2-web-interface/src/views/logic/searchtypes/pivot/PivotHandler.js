// @flow strict

export type Key = *;
export type Value = *;

type Keyed = {|
  key: Array<Key>,
|};

type SingleValue = {|
  rollup: boolean,
  value: Value,
|};

type MultiValue = {|
  // eslint-disable-next-line no-use-before-define
  values: Array<Row>,
|};

export type Leaf = {|
...{| source: 'leaf' |}, ...Keyed, ...MultiValue,
|};

export type NonLeaf = {|
...{| source: 'non-leaf' |}, ...Keyed, ...MultiValue,
|};

export type ColLeaf = {|
...{| source: 'col-leaf' |}, ...Keyed, ...SingleValue,
|};
export type RowLeaf = {|
...{| source: 'row-leaf' |}, ...Keyed, ...SingleValue,
|};
export type RowInner = {|
...{| source: 'row-inner' |}, ...Keyed, ...SingleValue,
|};

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
