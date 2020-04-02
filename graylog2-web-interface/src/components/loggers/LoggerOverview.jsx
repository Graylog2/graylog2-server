import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { NodeLoggers } from 'components/loggers';

import StoreProvider from 'injection/StoreProvider';

const LoggersStore = StoreProvider.getStore('Loggers');

const LoggerOverview = createReactClass({
  displayName: 'LoggerOverview',
  mixins: [Reflux.connect(LoggersStore)],

  render() {
    if (!this.state.loggers || !this.state.subsystems) {
      return <Spinner />;
    }
    const { subsystems } = this.state;
    const nodeLoggers = Object.keys(this.state.loggers)
      .map((nodeId) => (
        <NodeLoggers key={`node-loggers-${nodeId}`}
                     nodeId={nodeId}
                     subsystems={subsystems[nodeId] ? subsystems[nodeId].subsystems : {}} />
      ));
    return (
      <span>
        {nodeLoggers}
      </span>
    );
  },
});

export default LoggerOverview;
