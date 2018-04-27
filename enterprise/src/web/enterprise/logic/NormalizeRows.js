const _flatten = arrays => [].concat(...arrays);

const _normalizeRows = (rowPivots, columnPivots, series, rows, template = {}) => {
  if (rowPivots.length === 0) {
    return [];
  }

  const lastFieldname = rowPivots.shift();
  return _flatten(rows.map((r) => {
    const newTemplate = Object.assign({}, template);
    newTemplate[lastFieldname] = r[lastFieldname];
    if (rowPivots.length === 0) {
      series.forEach((s) => {
        newTemplate[s] = r[s];
      });
      columnPivots.forEach((columnPivot) => {
        newTemplate[columnPivot] = r[columnPivot];
      });
      return newTemplate;
    }
    const newRows = r[rowPivots[0]] ? r[rowPivots[0]] : [];
    return _normalizeRows(rowPivots.slice(), columnPivots.slice(), series, newRows, newTemplate);
  }));
};

const normalizeRows = (rowPivots, columnPivots, series, rows, template = {}) => _normalizeRows(rowPivots.slice(), columnPivots.slice(), series, rows, template);

export default normalizeRows;
