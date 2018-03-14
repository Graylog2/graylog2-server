const seriesRegex = /^(\w+)\((\w*)\)$/;

const _parseSeries = (s) => {
  const result = seriesRegex.exec(s);
  const definition = {
    type: result[1],
  };
  if (result[2] !== '') {
    definition.field = result[2];
  }
  return definition;
};

const _group = (fieldNames, series) => (fieldNames.length > 0 ? [{
  type: 'values',
  field: fieldNames.shift(),
  metrics: series.filter(s => s !== 'count()').map(s => _parseSeries(s)),
  groups: fieldNames.length > 0 ? _group(fieldNames, series) : [],
}] : []);

export default ({ rowPivots, series }) => {
  const fieldNames = rowPivots.slice();
  return [{
    type: 'aggregation',
    config: {
      groups: _group(fieldNames, series),
    },
  }];
};