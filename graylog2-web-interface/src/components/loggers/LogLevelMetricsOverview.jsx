import React from 'react';
import Reflux from 'reflux';

import LoggersStore from 'stores/system/LoggersStore';

import { LogLevelMetrics } from 'components/loggers';

const LogLevelMetricsOverview = React.createClass({
  propTypes: {
    nodeId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(LoggersStore)],
  render() {
    const { nodeId } = this.props;
    const logLevelMetrics = this.state.availableLoglevels
      .map((loglevel) => <LogLevelMetrics key={'loglevel-metrics-' + nodeId + '-' + loglevel} nodeId={nodeId} loglevel={loglevel} />);
    return (
      <div className="loglevel-metrics">
        {logLevelMetrics}
      </div>
    );
  },
});

export default LogLevelMetricsOverview;
