import React from 'react';
import PropTypes from 'prop-types';
import { Spinner } from 'components/common';
import HistogramVisualization from 'components/visualizations/HistogramVisualization';
import _ from 'lodash';
import moment from 'moment';
import crossfilter from 'crossfilter';
import numeral from 'numeral';
import DateTime from 'logic/datetimes/DateTime';

class TrafficGraph extends React.Component {
  static propTypes = {
    from: PropTypes.string.isRequired,
    to: PropTypes.string.isRequired,
    traffic: PropTypes.object.isRequired, // traffic is: {"2017-11-15T15:00:00.000Z": 68287229, ...}
    width: PropTypes.number.isRequired,
  };

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

    const ndx = crossfilter(_.map(this.props.traffic, (value, key) => {
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
                              width={this.props.width}
                              interactive
                              keyTitleRenderer={d => `<span class="date">${new DateTime(d.x).toString(DateTime.Formats.DATE)}</span>`}
                              valueTitleRenderer={d => `${numeral(d.y).format('0.00b')}`}
                              computationTimeRange={{ from: this.props.from, to: this.props.to }} />
    );
  }
}

export default TrafficGraph;
