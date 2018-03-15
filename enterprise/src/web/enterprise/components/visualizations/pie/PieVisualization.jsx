import React from 'react';
import PropTypes from 'prop-types';

import { AggregationType } from 'enterprise/components/aggregationbuilder/AggregationBuilderPropTypes';
import normalizeRows from 'enterprise/logic/NormalizeRows';
import GenericPlot from '../GenericPlot';

const _dimensions = (idx, total) => {
  const sliceSize = 1 / total;
  const leftSpacer = idx === 0 ? 0 : (sliceSize * 0.1);
  const rightSpacer = idx + 1 === total ? 0 : (sliceSize * 0.1);
  return [(sliceSize * idx) + leftSpacer, (sliceSize * (idx + 1)) - rightSpacer];
};
const _generateSeries = (config, data) => {
  const { series } = config;
  const results = normalizeRows(config.rowPivots.slice(), series, data, true);
  const x = results.map(v => config.rowPivots.map(p => v[p]).join('-'));
  return series.map((s, idx) => {
    const y = results.map(v => v[s]);
    return {
      type: 'pie',
      name: s,
      hole: 0.4,
      labels: x,
      values: y,
      domain: {
        x: _dimensions(idx, series.length),
        y: [0, 1],
      },
    };
  });
};

const PieVisualization = ({ config, data }) => <GenericPlot chartData={_generateSeries(config, data[0].results)} />;

PieVisualization.propTypes = {
  config: AggregationType.isRequired,
  data: PropTypes.arrayOf(PropTypes.object).isRequired,
};

export default PieVisualization;
