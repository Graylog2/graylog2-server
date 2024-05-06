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
    showlegend: true,
    barmode: 'overlay',
    margin: {
      l: 60,
    },
    xaxis: {
      type: 'date',
      title: {
        text: 'Time',
      },
      domain: [0.05, 0.95],
    },
    hovermode: 'x',
    yaxis: {
      title: {
        text: 'Days',
      },
      // side: 'left',
      // rangemode: 'tozero',
      type: 'date',
      tickformatstops: [
        { dtickrange: [0, 1000], value: '%S ms' }, // Milliseconds
        { dtickrange: [1000, 60000], value: '%M Min %S sec' }, // Seconds and minutes
        { dtickrange: [60000, 3600000], value: '%H H %M Min' }, // Minutes and hours
        { dtickrange: [3600000, 999999986400000], value: '%d days' }, // Hours and days
        // Add more custom tick formats as needed
      ],
      autoshift: true,
    },
    yaxis2: {
      title: {
        text: 'Size',
      },
      position: 0.05,
      // side: 'right',
      overlaying: 'y',
      // rangemode: 'tozero',
      hoverformat: '.2s',
      tickformat: 's',
      ticksuffix: 'bytes',
      autoshift: true,
    },
    yaxis3: {
      title: {
        text: 'Numbers',
      },
      overlaying: 'y',
      position: 0.95,
      anchor: 'free',
      side: 'right',
      // rangemode: 'tozero',
      autoshift: true,
    },
    updatemenus: layoutExtension.updatemenus && [...layoutExtension.updatemenus, ...layoutExtension.updatemenus, ...layoutExtension.updatemenus],
    annotations: layoutExtension.updatemenus && [...layoutExtension.annotations, ...layoutExtension.annotations, ...layoutExtension.annotations],
    shapes: layoutExtension.updatemenus && [...layoutExtension.shapes, ...layoutExtension.shapes, ...layoutExtension.shapes],
  };
  console.log({ layoutExtension, layout, chartData });

  return (
    <div style={{ height: '200px', width: width }}>
      <GenericPlot chartData={[
        {
          ...chartData[0],
          type: 'scatter',
          y: chartData[0].y.map((v) => v * Math.random()),
          text: chartData[0].y.map((v) => {
            if (v > 1000000) return `${v / 1000} sec`;

            return 'OOOFFFFOOOOOO';
          }),
          hovertemplate: 'Y-axis 1: %{text}<extra></extra>',
          yaxis: 'y1',
          hoverinfo: 'y',
        },
        { ...chartData[0], type: 'scatter', hoverinfo: 'y', yaxis: 'y2' },
        { ...chartData[0], type: 'scatter', hoverinfo: 'y', yaxis: 'y3', y: chartData[0].y.map((v) => v * Math.random()) },
      ]}
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
