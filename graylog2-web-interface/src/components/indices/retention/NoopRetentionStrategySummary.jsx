import React from 'react';

class NoopRetentionStrategySummary extends React.Component {
  render() {
    return (
      <div>
        <dl>
          <dt>Index retention strategy:</dt>
          <dd>Do nothing</dd>
        </dl>
      </div>
    );
  }
}

export default NoopRetentionStrategySummary;
