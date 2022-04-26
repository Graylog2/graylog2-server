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

import { Spinner } from 'components/common';
import GenericPlot from 'views/components/visualizations/GenericPlot';

type Props = {
  traffic: { [key: string]: number },
  width: number,
  layoutExtension?: {},
};

const TrafficGraph = ({ width, traffic, layoutExtension }: Props) => {
  if (!traffic) {
    return <Spinner />;
  }

  const chartData = [{
    type: 'bar',
    x: Object.keys(traffic),
    y: Object.values(traffic),
  }];
  const layout = {
    showlegend: false,
    margin: {
      l: 60,
    },
    xaxis: {
      type: 'date',
      title: {
        text: 'Time',
      },
    },
    hovermode: 'x',
    hoverlabel: {
      namelength: -1,
    },
    yaxis: {
      title: {
        text: 'Bytes',
      },
      rangemode: 'tozero',
      hoverformat: '.4s',
      tickformat: 's',
    },
    ...layoutExtension,
  };

  return (
    <div style={{ height: '200px', width: width }}>
      <GenericPlot chartData={chartData}
                   layout={layout} />
    </div>
  );
};

TrafficGraph.propTypes = {
  traffic: PropTypes.object.isRequired, // traffic is: {"2017-11-15T15:00:00.000Z": 68287229, ...}
  width: PropTypes.number.isRequired,
  layoutExtension: PropTypes.object,
};

TrafficGraph.defaultProps = {
  layoutExtension: {},
};

export default TrafficGraph;
