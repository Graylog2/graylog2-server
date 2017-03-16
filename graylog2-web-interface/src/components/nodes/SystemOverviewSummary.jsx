import React, { PropTypes } from 'react';
import StringUtils from 'util/StringUtils';

const SystemOverviewSummary = React.createClass({
  propTypes: {
    information: PropTypes.object.isRequired,
  },
  render() {
    const lbStatus = this.props.information.lb_status.toUpperCase();
    return (
      <dl className="graylog-node-state">
        <dt>Current lifecycle state:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(this.props.information.lifecycle)}</dd>
        <dt>Message processing:</dt>
        <dd>{this.props.information.is_processing ? 'Enabled' : 'Disabled'}</dd>
        <dt>Load balancer indication:</dt>
        <dd className={lbStatus === 'DEAD' ? 'text-danger' : ''}>{lbStatus}</dd>
      </dl>
    );
  },
});

export default SystemOverviewSummary;
