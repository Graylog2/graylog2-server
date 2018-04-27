import { pivotForField } from './PivotGenerator';

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

const formatInterval = ({ value, unit }) => `${value}${unit[0]}`;

const _typeForField = (field, { interval }) => {
  switch (field) {
    case 'timestamp':
      return { type: 'time', interval: formatInterval(interval) };
    default:
      return { type: 'values' };
  }
};

const _groupDefinition = (pivot, series, groups) => {
  const { field, config } = pivot;
  return Object.assign(
    {
      field,
      metrics: series.map(s => _parseSeries(s)),
      groups,
    },
    _typeForField(field, config),
  );
};

const _group = (rowPivots, columnPivots, series) => {
  if (rowPivots.length > 0) {
    return [_groupDefinition(rowPivots.shift(), series, rowPivots.length >= 0 ? _group(rowPivots, columnPivots, series) : [])];
  }
  return columnPivots.map(pivot => _groupDefinition(pivot, series, []));
};

export default ({ columnPivots, rowPivots, series }) => {
  return [{
    type: 'aggregation',
    config: {
      groups: _group(rowPivots.slice(0), columnPivots.slice(0), series),
    },
  }];
};