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

import NumberUtils from 'util/NumberUtils';
import { Spinner } from 'components/common';
import GenericPlot from 'views/components/visualizations/GenericPlot';

type Props = {
  traffic: { [key: string]: number },
  width: number,
  layoutExtension?: {},
};

const fileSizeUnits = ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];

const fileSize = (bytes) => {
  const thresh = 1024;

  if (Math.abs(bytes) < thresh) {
    return { value: bytes, unit: 'B' };
  }

  let u = -1;
  const r = 10 ** 1;

  do {
    bytes /= thresh;
    ++u;
  } while (Math.round(Math.abs(bytes) * r) / r >= thresh && u < fileSizeUnits.length - 1);

  return { value: +bytes.toFixed(1), unit: fileSizeUnits[u] };
};

console.log(fileSize(808071624));

const TrafficGraph = ({ width, traffic, layoutExtension }: Props) => {
  if (!traffic) {
    return <Spinner />;
  }

  const customData = [808071624, 4534, 912308071624, 543824, 54382654, 237446, 59834, 299485662378523, 234];

  const formattedTrafficData = customData.map(((bytes) => fileSize(bytes)));

  console.log('===formattedTrafficData', formattedTrafficData);

  console.log('===formattedTrafficData 3', formattedTrafficData.map((data) => data.value));

  // traffic data

  const trafficUnits = formattedTrafficData.map(((data) => data.unit));

  console.log('===trafficUnits', trafficUnits);

  const sortedTrafficUnits = trafficUnits.sort((a, b) => fileSizeUnits.indexOf(a) - fileSizeUnits.indexOf(b));

  console.log('===sortedTrafficUnits', sortedTrafficUnits);

  const biggestUnit = sortedTrafficUnits[sortedTrafficUnits.length - 1];

  console.log('===biggestUnit', biggestUnit);

  console.log('====traffic numbers', Object.keys(traffic).length);

  const chartData = [{
    type: 'bar',
    x: Object.keys(traffic),
    y: customData,
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
      hoverformat: '2B',
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
