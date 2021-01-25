/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import moment from 'moment';

import Plot from 'views/components/visualizations/plotly/AsyncPlot';

const _formatTimestamp = (epoch) => {
  return moment.unix(epoch).format('YYYY-MM-DD HH:mm:ss');
};

const _generateSeries = (results) => {
  const data = new Immutable.OrderedMap(results);

  return [{
    type: 'bar',
    x: data.keySeq().map(_formatTimestamp).toArray(),
    y: data.valueSeq().toArray(),
    name: 'took_ms',
  }];
};

export default function Histogram({ data }) {
  return (
    <Plot data={_generateSeries(data.results)}
          style={{ position: 'absolute' }}
          fit
          layout={{
            margin: {
              t: 10,
              pad: 10,
            },
          }}
          config={{ displayModeBar: false }} />
  );
}

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
