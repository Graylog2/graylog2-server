import React from 'react';

const NoopRetentionStrategySummary = React.createClass({
  render() {
    return (
      <div>
        <dl>
          <dt>Index retention strategy:</dt>
          <dd>Do nothing</dd>
        </dl>
      </div>
    );
  },
});

export default NoopRetentionStrategySummary;
