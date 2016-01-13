import React, {PropTypes} from 'react';
import StringUtils from 'util/StringUtils';

const SystemOverview = React.createClass({
  propTypes: {
    information: PropTypes.object.isRequired,
  },
  render() {
    return (
      <dl className="graylog-node-state">
        <dt>Current lifecycle state:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(this.props.information.lifecycle)}</dd>
        <dt>Message processing:</dt>
        <dd>{this.props.information.is_processing ? 'Enabled' : 'Disabled'}</dd>
        <dt>Load balancer indication:</dt>
        <dd>{this.props.information.lb_status.toUpperCase()}</dd>
      </dl>
    );
  },
});

export default SystemOverview;
