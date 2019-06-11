import { flatten, setWith } from 'lodash';

const expandRows = (fieldNames, columnFieldNames, rows) => {
  if (!rows) {
    return [];
  }

  const expanded = [];

  rows.forEach((row) => {
    const { values } = row;
    const result = {};

    row.key.forEach((key, idx) => {
      result[fieldNames[idx]] = key;
    });

    values.forEach(({ key, value }) => {
      const translatedKeys = flatten(key.map((k, idx) => (idx < key.length - 1 && columnFieldNames[idx] ? [columnFieldNames[idx], k] : k)));
      setWith(result, translatedKeys, value, Object);
    });
    expanded.push(result);
  });
  return expanded;
};

export default expandRows;
