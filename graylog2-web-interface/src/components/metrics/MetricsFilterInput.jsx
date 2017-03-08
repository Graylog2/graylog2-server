import React from 'react';

const MetricsFilterInput = React.createClass({
  render() {
    return (
      <input type="text" className="metrics-filter input-lg form-control"
             style={{ width: '100%' }} placeholder="Type a metric name to filter..." {...this.props} />
    );
  },
});

export default MetricsFilterInput;
