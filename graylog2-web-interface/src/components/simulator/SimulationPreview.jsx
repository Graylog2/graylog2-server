import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from 'components/graylog';

import MessageShow from 'components/search/MessageShow';

const SimulationPreview = ({ simulationResults, streams }) => {
  const { messages } = simulationResults;

  if (messages.length === 0) {
    return (
      <Alert bsStyle="info">
        <p><strong>Message would be dropped</strong></p>
        <p>
          The pipeline processor would drop such a message. That means that the message
          <strong>would not be stored</strong>, and would not be available for searches, alerts, outputs, or dashboards.
        </p>
      </Alert>
    );
  }

  const formattedMessages = messages.map((message) => {
    return (
      <MessageShow key={message.id}
                   message={message}
                   streams={streams} />
    );
  });

  return <div className="message-preview-wrapper">{formattedMessages}</div>;
};

SimulationPreview.propTypes = {
  simulationResults: PropTypes.object.isRequired,
  streams: PropTypes.object.isRequired,
};

export default SimulationPreview;
