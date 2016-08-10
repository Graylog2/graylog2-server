import React from 'react';
import { Col, Row } from 'react-bootstrap';

import RawMessageLoader from 'components/messageloaders/RawMessageLoader';
import SimulationResults from './SimulationResults';

import SimulatorActions from './SimulatorActions';
// eslint-disable-next-line no-unused-vars
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

  _onMessageLoad(message, options) {
    this.setState({ message: message, simulation: undefined, loading: true, error: undefined });

    SimulatorActions.simulate
      .triggerPromise(this.props.stream, message.fields, options.inputId)
      .then(
        response => {
          this.setState({ simulation: response, loading: false });
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
            <p>
              Build an example message that will be used in the simulation.{' '}
              <strong>No real messages stored in Graylog will be changed. All actions are purely simulated on the temporary input you provide below.</strong>
            </p>
            <RawMessageLoader onMessageLoaded={this._onMessageLoad} inputIdSelector />
          </Col>
        </Row>
        <SimulationResults stream={this.props.stream}
                           originalMessage={this.state.message}
                           simulationResults={this.state.simulation}
                           isLoading={this.state.loading}
                           error={this.state.error} />
      </div>
    );
  },
});

export default ProcessorSimulator;
