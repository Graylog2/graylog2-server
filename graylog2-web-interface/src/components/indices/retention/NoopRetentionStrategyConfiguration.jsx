import React from 'react';
import { Alert } from 'react-bootstrap';

const NoopRetentionStrategyConfiguration = React.createClass({
  render() {
    return (
      <Alert>
        This retention strategy is not configurable because it does not do anything.
      </Alert>
    );
  },
});

export default NoopRetentionStrategyConfiguration;
