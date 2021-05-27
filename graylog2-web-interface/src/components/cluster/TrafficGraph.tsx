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
import _ from 'lodash';
import moment from 'moment';
import crossfilter from 'crossfilter';

import { Spinner } from 'components/common';
import GenericPlot from 'views/components/visualizations/GenericPlot';

type Props = {
  traffic: { [key: string]: number },
  width: number,
};

const TrafficGraph = ({ width, traffic }: Props) => {
  if (!traffic) {
    return <Spinner />;
  }

  const ndx = crossfilter(_.map(traffic, (value, key) => ({ ts: key, bytes: value })));
  const dailyTraffic = ndx.dimension((d) => moment(d.ts).format('YYYY-MM-DD'));

  const dailySums = dailyTraffic.group().reduceSum((d) => d.bytes);
  const t = _.mapKeys(dailySums.all(), (entry) => moment.utc(entry.key, 'YYYY-MM-DD').toISOString());
  const unixTraffic = _.mapValues(t, (val) => val.value);
  const chartData = [{
    type: 'bar',
    x: Object.keys(unixTraffic),
    y: Object.values(unixTraffic),
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
    yaxis: {
      title: {
        text: 'Bytes',
      },
      rangemode: 'tozero',
      hoverformat: '.4s',
      tickformat: 's',
    },
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
};

export default TrafficGraph;
