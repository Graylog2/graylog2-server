import React from 'react';
import numeral from 'numeral';

const CounterDetails = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
  },
  render() {
    const metric = this.props.metric.metric;
    return (
      <dl className="metric-def metric-counter">
        <dt>Value:</dt>
        <dd><span className="number-format">{numeral(metric.count).format('0,0')}</span></dd>
      </dl>
    );
  },
});

export default CounterDetails;
