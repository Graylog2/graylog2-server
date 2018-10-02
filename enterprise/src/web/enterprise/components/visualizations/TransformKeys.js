import moment from 'moment-timezone';
import CombinedProvider from 'injection/CombinedProvider';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

const formatTimestamp = (timestamp, tz = 'UTC') => {
  // the `true` parameter prevents returning the iso string in UTC (http://momentjs.com/docs/#/displaying/as-iso-string/)
  return moment(timestamp).tz(tz).toISOString(true);
};

const transformKey = (key, indices, tz) => {
  if (indices.length === 0) {
    return key;
  }
  const newKey = key.slice();
  indices.forEach((idx) => {
    if (newKey[idx]) {
      newKey[idx] = formatTimestamp(newKey[idx], tz);
    }
  });
  return newKey;
};

const findIndices = (ary, predicate) => ary.map((value, idx) => ({ value, idx })).filter(({ value }) => predicate(value)).map(({ idx }) => idx);
export default (rowPivots, columnPivots, result) => {
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
    const newRow = Object.assign({}, row);
    newRow.key = transformKey(row.key, rowIndices, tz);

    if (columnIndices.length > 0) {
      newRow.values = row.values.map((values) => {
        if (values.source !== 'col-leaf') {
          return values;
        }
        const newValues = Object.assign({}, values);
        newValues.key = transformKey(values.key, columnIndices, tz);
        return newValues;
      });
    }

    return newRow;
  });
};
