// @flow strict
import moment from 'moment-timezone';

import CombinedProvider from 'injection/CombinedProvider';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import type { ColLeaf, Leaf, Key, Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const formatTimestamp = (timestamp, tz = 'UTC'): string => {
  // the `true` parameter prevents returning the iso string in UTC (http://momentjs.com/docs/#/displaying/as-iso-string/)
  return moment(timestamp).tz(tz || 'UTC').toISOString(true);
};

const transformKey = (key: Key, indices: Array<number>, tz: string) => {
  if (indices.length === 0) {
    return key;
  }
  const newKey: Key = key.slice();
  indices.forEach((idx) => {
    if (newKey[idx]) {
      newKey[idx] = formatTimestamp(newKey[idx], tz);
    }
  });
  return newKey;
};

const findIndices = <T>(ary: Array<T>, predicate: (T) => boolean): Array<number> => ary
  .map((value, idx) => ({ value, idx }))
  .filter(({ value }) => predicate(value))
  .map(({ idx }) => idx);

export default (rowPivots: Array<Pivot>, columnPivots: Array<Pivot>): ((Rows) => Rows) => {
  return (result = []) => {
    const rowIndices = findIndices(rowPivots, pivot => (pivot.type === 'time'));
    const columnIndices = findIndices(columnPivots, pivot => (pivot.type === 'time'));

    if (rowIndices.length === 0 && columnIndices.length === 0) {
      return result;
    }
    const currentUser = CurrentUserStore.get();
    const tz = currentUser ? currentUser.timezone : 'UTC';

    return result.map((row) => {
      if (row.source !== 'leaf') {
        return row;
      }
      const newRow: Leaf = { ...row };
      newRow.key = transformKey(row.key, rowIndices, tz);

      if (columnIndices.length > 0) {
        newRow.values = row.values.map((values) => {
          if (values.source !== 'col-leaf') {
            return values;
          }
          const newValues: ColLeaf = { ...values };
          newValues.key = transformKey(values.key, columnIndices, tz);
          return newValues;
        });
      }

      return newRow;
    });
  };
};
