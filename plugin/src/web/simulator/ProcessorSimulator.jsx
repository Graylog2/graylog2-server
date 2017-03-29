import React from 'react';
import { Col, ControlLabel, FormGroup, HelpBlock, Panel, Row } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';
import { LinkContainer } from 'react-router-bootstrap';

import { Select } from 'components/common';
import RawMessageLoader from 'components/messageloaders/RawMessageLoader';
import SimulationResults from './SimulationResults';

import Routes from 'routing/Routes';

import SimulatorActions from './SimulatorActions';
// eslint-disable-next-line no-unused-vars
import SimulatorStore from './SimulatorStore';

const DEFAULT_STREAM_ID = '000000000000000000000001';

const ProcessorSimulator = React.createClass({
  propTypes: {
    streams: React.PropTypes.array.isRequired,
  },

  getInitialState() {
    // The default stream could not be present in a system. In that case we fallback to the first available stream.
    this.defaultStream = this.props.streams.find(s => s.id === DEFAULT_STREAM_ID) || this.props.streams[0];

    return {
      message: undefined,
      stream: this.defaultStream,
      simulation: undefined,
      loading: false,
      error: undefined,
    };
  },

  _onMessageLoad(message, options) {
    this.setState({ message: message, simulation: undefined, loading: true, error: undefined });

    SimulatorActions.simulate
      .triggerPromise(this.state.stream, message.fields, options.inputId)
      .then(
        response => {
          this.setState({ simulation: response, loading: false });
        },
        error => {
          this.setState({ loading: false, error: error });
        }
      );
  },

  _getFormattedStreams(streams) {
    if (!streams) {
      return [];
    }

    return streams
      .map(s => {
        return { value: s.id, label: s.title };
      })
      .sort((s1, s2) => naturalSort(s1.label, s2.label));
  },

  _onStreamSelect(selectedStream) {
    const stream = this.props.streams.find(s => s.id.toLowerCase() === selectedStream.toLowerCase());
    this.setState({ stream: stream });
  },

  render() {
    if (this.props.streams.length === 0) {
      return (
        <div>
          <Row className="row-sm">
            <Col md={8} mdOffset={2}>
              <Panel bsStyle="danger" header="No streams found">
                Pipelines operate on streams, but your system currently has no streams. Please{' '}
                <LinkContainer to={Routes.STREAMS}><a>create a stream</a></LinkContainer>{' '}
                and come back here later to test pipelines processing messages in your new stream.
              </Panel>
            </Col>
          </Row>
        </div>
      );
    }

    const streamHelp = (
      <span>
        Select a stream to use during simulation, the <em>{this.defaultStream.title}</em> stream is used by default.
      </span>
    );

    return (
      <div>
        <Row>
          <Col md={12}>
            <h1>Load a message</h1>
            <p>
              Build an example message that will be used in the simulation.{' '}
              <strong>No real messages stored in Graylog will be changed. All actions are purely simulated on the
                temporary input you provide below.</strong>
            </p>
            <Row className="row-sm">
              <Col md={7}>
                <FormGroup id="streamSelectorSimulation">
                  <ControlLabel>Stream</ControlLabel>
                  <Select options={this._getFormattedStreams(this.props.streams)}
                          onValueChange={this._onStreamSelect} value={this.state.stream.id} clearable={false} />
                  <HelpBlock>{streamHelp}</HelpBlock>
                </FormGroup>
              </Col>
            </Row>
            <RawMessageLoader onMessageLoaded={this._onMessageLoad} inputIdSelector />
          </Col>
        </Row>
        <SimulationResults stream={this.state.stream}
                           originalMessage={this.state.message}
                           simulationResults={this.state.simulation}
                           isLoading={this.state.loading}
                           error={this.state.error} />
      </div>
    );
  },
});

export default ProcessorSimulator;
