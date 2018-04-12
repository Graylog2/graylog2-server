const _flatten = arrays => [].concat(...arrays);

const _normalizeRows = (fieldNames, series, rows, template = {}) => {
  if (fieldNames.length === 0) {
    return [];
  }

  const lastFieldname = fieldNames.shift();
  return _flatten(rows.map((r) => {
    const newTemplate = Object.assign({}, template);
    newTemplate[lastFieldname] = r[lastFieldname];
    if (fieldNames.length === 0) {
      series.forEach((s) => {
        newTemplate[s] = r[s];
      });
      return newTemplate;
    }
    const newRows = r[fieldNames[0]] ? r[fieldNames[0]] : [];
    return normalizeRows(fieldNames.slice(), series, newRows, newTemplate);
  }));
};

const normalizeRows = (fieldNames, series, rows, template = {}) => _normalizeRows(fieldNames.slice(), series, rows, template);

export default normalizeRows;
