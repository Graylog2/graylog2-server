import React from 'react';

type MessageCountRotationStrategySummaryProps = {
  config: any;
};

const MessageCountRotationStrategySummary = ({
  config
}: MessageCountRotationStrategySummaryProps) => (
  <div>
    <dl>
      <dt>Index rotation strategy:</dt>
      <dd>Message Count</dd>
      <dt>Max docs per index:</dt>
      <dd>{config.max_docs_per_index}</dd>
    </dl>
  </div>
);

export default MessageCountRotationStrategySummary;
