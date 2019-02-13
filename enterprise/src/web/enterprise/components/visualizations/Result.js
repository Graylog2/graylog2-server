// @flow strict
export type Key = Array<string>;

export type Source = 'leaf' | 'col-leaf';
export type Value = {
  source: Source,
  key: Key,
};

export type Row = {
  source: Source,
  values: Array<Value>,
  key: Key,
}
export type Result = Array<Row>;
