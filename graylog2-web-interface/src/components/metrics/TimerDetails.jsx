import React from 'react';
import numeral from 'numeral';

const TimerDetails = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
  },
  render() {
    const timing = this.props.metric.metric.time;
    return (
      <dl className="metric-def metric-timer">
        <dt>95th percentile:</dt>
        <dd><span>{numeral(timing['95th_percentile']).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>98th percentile:</dt>
        <dd><span>{numeral(timing['98th_percentile']).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>99th percentile:</dt>
        <dd><span>{numeral(timing['99th_percentile']).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>Standard deviation:</dt>
        <dd><span>{numeral(timing.std_dev).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>Mean:</dt>
        <dd><span>{numeral(timing.mean).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>Minimum:</dt>
        <dd><span>{numeral(timing.min).format('0,0.[00]')}</span>&#956;s</dd>

        <dt>Maximum:</dt>
        <dd><span>{numeral(timing.max).format('0,0.[00]')}</span>&#956;s</dd>
      </dl>
    );
  },
});

export default TimerDetails;
