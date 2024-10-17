import React from 'react';

import { Alert } from 'components/bootstrap';
import MessageShow from 'components/search/MessageShow';

type SimulationPreviewProps = {
  simulationResults: any;
  streams: any;
};

const SimulationPreview = ({
  simulationResults,
  streams,
}: SimulationPreviewProps) => {
  const { messages } = simulationResults;

  if (messages.length === 0) {
    return (
      <Alert bsStyle="info" title="Message would be dropped">
        <p>
          The pipeline processor would drop such a message. That means that the message
          <strong>would not be stored</strong>, and would not be available for searches, alerts, outputs, or dashboards.
        </p>
      </Alert>
    );
  }

  const formattedMessages = messages.map((message) => (
    <MessageShow key={message.id}
                 message={message}
                 streams={streams} />
  ));

  return <div className="message-preview-wrapper">{formattedMessages}</div>;
};

export default SimulationPreview;
