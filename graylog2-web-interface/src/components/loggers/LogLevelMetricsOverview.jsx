import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';

import { LogLevelMetrics } from 'components/loggers';

const LoggersStore = StoreProvider.getStore('Loggers');

const LogLevelMetricsOverview = createReactClass({
  displayName: 'LogLevelMetricsOverview',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(LoggersStore)],

  render() {
    const { nodeId } = this.props;
    const logLevelMetrics = this.state.availableLoglevels
      .map((loglevel) => <LogLevelMetrics key={`loglevel-metrics-${nodeId}-${loglevel}`} nodeId={nodeId} loglevel={loglevel} />);
    return (
      <div className="loglevel-metrics">
        {logLevelMetrics}
      </div>
    );
  },
});

export default LogLevelMetricsOverview;
