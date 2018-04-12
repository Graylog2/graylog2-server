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

const _group = (fieldNames, series) => {
  if (fieldNames.length > 0) {
    const { field, config } = fieldNames.shift();
    return [
      Object.assign({
        field,
        metrics: series.map(s => _parseSeries(s)),
        groups: fieldNames.length > 0 ? _group(fieldNames, series) : [],
      },
      _typeForField(field, config)),
    ];
  }
  return [];
};

export default ({ rowPivots, series }) => {
  const fieldNames = rowPivots.slice();
  return [{
    type: 'aggregation',
    config: {
      groups: _group(fieldNames, series),
    },
  }];
};