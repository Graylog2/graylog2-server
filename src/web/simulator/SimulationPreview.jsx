import React from 'react';
import { Alert, Col, Row } from 'react-bootstrap';

import { Spinner } from 'components/common';
import MessageShow from 'components/search/MessageShow';

const SimulationPreview = React.createClass({
  propTypes: {
    stream: React.PropTypes.object.isRequired,
    originalMessage: React.PropTypes.object,
    simulationResults: React.PropTypes.array,
    isLoading: React.PropTypes.bool,
    error: React.PropTypes.object,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  style: require('!style/useable!css!./SimulationPreview.css'),

  render() {
    if (!this.props.originalMessage && !this.props.simulationResults) {
      return null;
    }

    const streams = {};
    streams[this.props.stream.id] = this.props.stream;

    let originalMessagePreview = (this.props.isLoading ? <Spinner /> : null);
    if (this.props.originalMessage) {
      originalMessagePreview = (
        <MessageShow message={this.props.originalMessage}
                     streams={streams}
                     disableTestAgainstStream
                     disableSurroundingSearch
                     disableFieldActions />
      );
    }

    let simulationPreview = (this.props.isLoading ? <Spinner /> : null);
    if (this.props.simulationResults && Array.isArray(this.props.simulationResults)) {
      if (this.props.simulationResults.length === 0) {
        simulationPreview = (
          <Alert bsStyle="info">
            <p><strong>Message would be dropped</strong></p>
            <p>
              Processing the loaded message would drop it from the system. That means that the message <strong>would
              not be stored</strong>, and would not be available on searches, alerts, or dashboards.
            </p>
          </Alert>
        );
      } else {
        const messages = this.props.simulationResults.map(message => {
          return (
            <MessageShow key={message.id}
                         message={message}
                         streams={streams}
                         disableTestAgainstStream
                         disableSurroundingSearch
                         disableFieldActions
                         disableMessageActions />
          );
        });
        simulationPreview = <div className="message-preview-wrapper">{messages}</div>;
      }
    }

    let errorMessage;
    if (this.props.error) {
      errorMessage = (
        <Alert bsStyle="danger">
          <p><strong>Error simulating message processing</strong></p>
          <p>
            Could not simulate processing of message <em>{this.props.originalMessage.id}</em> in stream{' '}
            <em>{this.props.stream.title}</em>.
            <br />
            Please try loading the message again, or use another message for the simulation.
          </p>
        </Alert>
      );
    }

    return (
      <Row>
        <Col md={12}>
          <hr />
        </Col>
        <Col md={6}>
          <h1>Original message</h1>
          <p>This is the original message loaded from Graylog.</p>
          <div className="message-preview-wrapper">
            {originalMessagePreview}
          </div>
        </Col>
        <Col md={6}>
          <h1>Simulation results</h1>
          <p>This is the result of processing the loaded message:</p>
          {errorMessage}
          {simulationPreview}
        </Col>
      </Row>
    );
  },
});

export default SimulationPreview;
