import React from 'react';
import { Col, Row } from 'react-bootstrap';

import LoaderTabs from 'components/messageloaders/LoaderTabs';
import SimulationPreview from './SimulationPreview';

import SimulatorActions from './SimulatorActions';
import SimulatorStore from './SimulatorStore';

const ProcessorSimulator = React.createClass({
  propTypes: {
    stream: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      message: undefined,
      simulation: undefined,
      loading: false,
      error: undefined,
    };
  },

  _onMessageLoad(message) {
    this.setState({ message: message, simulation: undefined, loading: true, error: undefined });

    SimulatorActions.simulate.triggerPromise(this.props.stream, message.index, message.id).then(
      messages => {
        this.setState({ simulation: messages, loading: false });
      },
      error => {
        this.setState({ loading: false, error: error });
      }
    );
  },

  render() {
    return (
      <div>
        <Row>
          <Col md={12}>
            <h1>Load a message</h1>
            <p>Load a message to be used in the simulation. <strong>No changes will be done in your stored
              messages.</strong></p>
            <LoaderTabs onMessageLoaded={this._onMessageLoad} disableMessagePreview />
          </Col>
        </Row>
        <SimulationPreview stream={this.props.stream}
                           originalMessage={this.state.message}
                           simulationResults={this.state.simulation}
                           isLoading={this.state.loading}
                           error={this.state.error} />
      </div>
    );
  },
});

export default ProcessorSimulator;
