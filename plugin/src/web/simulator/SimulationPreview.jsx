import React from 'react';
import { Alert } from 'react-bootstrap';

import MessageShow from 'components/search/MessageShow';

const SimulationPreview = React.createClass({
  propTypes: {
    simulationResults: React.PropTypes.object.isRequired,
    streams: React.PropTypes.object.isRequired,
  },

  render() {
    const messages = this.props.simulationResults.messages;

    if (messages.length === 0) {
      return (
        <Alert bsStyle="info">
          <p><strong>Message would be dropped</strong></p>
          <p>
            The pipeline processor would drop such a message. That means that the message <strong>would
            not be stored</strong>, and would not be available for searches, alerts, outputs, or dashboards.
          </p>
        </Alert>
      );
    }

    const formattedMessages = messages.map(message => {
      return (
        <MessageShow key={message.id}
                     message={message}
                     streams={this.props.streams}
                     disableTestAgainstStream
                     disableSurroundingSearch
                     disableFieldActions
                     disableMessageActions />
      );
    });

    return <div className="message-preview-wrapper">{formattedMessages}</div>;
  },
});

export default SimulationPreview;
