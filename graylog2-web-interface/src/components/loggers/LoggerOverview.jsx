import React from 'react';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { NodeLoggers } from 'components/loggers';

import StoreProvider from 'injection/StoreProvider';
const LoggersStore = StoreProvider.getStore('Loggers');

const LoggerOverview = React.createClass({
  mixins: [Reflux.connect(LoggersStore)],
  render() {
    if (!this.state.loggers || !this.state.subsystems) {
      return <Spinner />;
    }
    const nodeLoggers = Object.keys(this.state.loggers)
      .map((nodeId) => <NodeLoggers key={'node-loggers-' + nodeId}
                                    nodeId={nodeId}
                                    subsystems={this.state.subsystems[nodeId].subsystems}/>);
    return (
      <span>
        {nodeLoggers}
      </span>
    );
  },
});

export default LoggerOverview;
