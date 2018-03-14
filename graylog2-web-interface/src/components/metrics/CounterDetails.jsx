import PropTypes from 'prop-types';
import React from 'react';
import numeral from 'numeral';

class CounterDetails extends React.Component {
  static propTypes = {
    metric: PropTypes.object.isRequired,
  };

  render() {
    const metric = this.props.metric.metric;
    return (
      <dl className="metric-def metric-counter">
        <dt>Value:</dt>
        <dd><span className="number-format">{numeral(metric.count).format('0,0')}</span></dd>
      </dl>
    );
  }
}

export default CounterDetails;
