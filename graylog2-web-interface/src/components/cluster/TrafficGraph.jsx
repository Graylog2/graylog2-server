import React from 'react';
import PropTypes from 'prop-types';
import { Spinner } from 'components/common';
import HistogramVisualization from 'components/visualizations/HistogramVisualization';
import _ from 'lodash';
import moment from 'moment';
import crossfilter from 'crossfilter';

const TrafficGraph = React.createClass({

  propTypes: {
    traffic: PropTypes.object.isRequired,
  },

  render() {
    if (!this.props.traffic) {
      return <Spinner />;
    }

    const config = {
      xAxis: 'Time',
      yAxis: 'Bytes',
      interval: 'days',
      timerange: {
        type: 'relative',
        range: 60 * 24 * 30, // 30 days, value is irrelevant but it must not be 0
      },
    };

    const traffic = this.props.traffic;
    const ndx = crossfilter(_.map(traffic.output, (value, key) => {
      return { ts: key, bytes: value };
    }));
    const dailyTraffic = ndx.dimension((d) => {
      return moment(d.ts).format('YYYY-MM-DD');
    });
    const dailySums = dailyTraffic.group().reduceSum(d => d.bytes);
    const t = _.mapKeys(dailySums.all(), entry => moment.utc(entry.key, 'YYYY-MM-DD').unix());
    const unixTraffic = _.mapValues(t, val => val.value);

    return (
      <HistogramVisualization id="traffic-histogram"
                              data={unixTraffic}
                              config={config}
                              width={800}
                              interactive={false}
                              computationTimeRange={{ from: traffic.from, to: traffic.to }} />
    );
  },
});

export default TrafficGraph;
