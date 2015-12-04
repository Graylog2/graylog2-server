import React from 'react';
import numeral from 'numeral';

const GaugeDetails = React.createClass({
  propTypes: {
    metric: React.PropTypes.object.isRequired,
  },
  render() {
    const gauge = this.props.metric.metric;
    return (
      <dl className="metric-def metric-gauge">
        <dt>Value:</dt>
        <dd><span className="number-format">{numeral(gauge.value).format('0,0')}</span></dd>
      </dl>
    );
  },
});

export default GaugeDetails;
