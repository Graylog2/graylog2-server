const expandRows = (fieldNames, series, rows, expanded = []) => {
  if (fieldNames.length === 0) {
    return [];
  }

  const fieldName = fieldNames.shift();
  rows.forEach((row) => {
    const result = {
      count: row.count,
    };
    series.forEach((seriesName) => {
      result[seriesName] = row[seriesName];
    });
    result[fieldName] = row[fieldName];
    expanded.push(result);

    if (fieldNames.length > 0 && row[fieldNames[0]]) {
      expandRows(fieldNames.slice(), series, row[fieldNames[0]], expanded);
    }
  });
  return expanded;
};

export default expandRows;