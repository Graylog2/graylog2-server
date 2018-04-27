const expandRows = (fieldNames, columnFieldNames, series, rows, expanded = []) => {
  if (fieldNames.length === 0) {
    return [];
  }

  const fieldName = fieldNames.shift();
  rows.forEach((row) => {
    const result = {};

    series.forEach((seriesName) => {
      result[seriesName] = row[seriesName];
    });
    result[fieldName] = row[fieldName];

    if (fieldNames.length === 0 && columnFieldNames.length > 0) {
      columnFieldNames.forEach((columnFieldName) => {
        const columnPivotField = row[columnFieldName] || [];
        const columnPivotValueMap = {};
        if (columnPivotField && columnPivotField.forEach) {
          columnPivotField.forEach((v) => {
            columnPivotValueMap[v[columnFieldName]] = Object.assign({}, v);
          });
          result[columnFieldName] = columnPivotValueMap;
        }
      });
    }

    expanded.push(result);

    if (fieldNames.length > 0 && row[fieldNames[0]]) {
      expandRows(fieldNames.slice(), columnFieldNames, series, row[fieldNames[0]], expanded);
    }
  });
  return expanded;
};

export default expandRows;