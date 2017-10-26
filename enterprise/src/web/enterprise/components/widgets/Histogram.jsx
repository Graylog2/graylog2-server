import React from 'react';
import PropTypes from 'prop-types';

import HistogramVisualization from 'components/visualizations/HistogramVisualization';

export default function Histogram(props) {
  return (<HistogramVisualization {...props}
                                 computationTimeRange={props.data.timerange}
                                 config={Object.assign(props.config, {
                                   interval: props.data.interval,
                                 })}
                                 data={props.data.results} />);
};

Histogram.propTypes = {
  data: PropTypes.shape({
    config: PropTypes.shape({
      timerange: PropTypes.object.isRequired,
    }).isRequired,
    interval: PropTypes.string.isRequired,
    timerange: PropTypes.object.isRequired,
    results: PropTypes.object.isRequired,
  }).isRequired,
};
