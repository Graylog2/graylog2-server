import PropTypes from 'prop-types';
import React from 'react';
import { Col, ControlLabel, FormGroup, HelpBlock, Panel, Row } from 'components/graylog';
import naturalSort from 'javascript-natural-sort';
import { Link } from 'react-router';

import { Select } from 'components/common';
import RawMessageLoader from 'components/messageloaders/RawMessageLoader';

import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import SimulationResults from './SimulationResults';

const { SimulatorActions } = CombinedProvider.get('Simulator');

const DEFAULT_STREAM_ID = '000000000000000000000001';

class ProcessorSimulator extends React.Component {
  static propTypes = {
    streams: PropTypes.array.isRequired,
  };

  constructor(props) {
    super(props);
    // The default stream could not be present in a system. In that case we fallback to the first available stream.
    this.defaultStream = props.streams.find((s) => s.id === DEFAULT_STREAM_ID) || props.streams[0];

    this.state = {
      message: undefined,
      stream: this.defaultStream,
      simulation: undefined,
      loading: false,
      error: undefined,
    };
  }

  _onMessageLoad = (message, options) => {
    this.setState({ message: message, simulation: undefined, loading: true, error: undefined });

    SimulatorActions.simulate
      .triggerPromise(this.state.stream, message.fields, options.inputId)
      .then(
        (response) => {
          this.setState({ simulation: response, loading: false });
        },
        (error) => {
          this.setState({ loading: false, error: error });
        },
      );
  };

  _getFormattedStreams = (streams) => {
    if (!streams) {
      return [];
    }

    return streams
      .map((stream) => {
        return { value: stream.id, label: stream.title };
      })
      .sort((s1, s2) => naturalSort(s1.label, s2.label));
  };

  _onStreamSelect = (selectedStream) => {
    const stream = this.props.streams.find((s) => s.id.toLowerCase() === selectedStream.toLowerCase());
    this.setState({ stream: stream });
  };

  render() {
    if (this.props.streams.length === 0) {
      return (
        <div>
          <Row className="row-sm">
            <Col md={8} mdOffset={2}>
              <Panel bsStyle="danger" header="No streams found">
                Pipelines operate on streams, but your system currently has no streams. Please{' '}
                <Link to={Routes.STREAMS}>create a stream</Link>{' '}
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
                temporary input you provide below.
              </strong>
            </p>
            <Row className="row-sm">
              <Col md={7}>
                <FormGroup id="streamSelectorSimulation">
                  <ControlLabel>Stream</ControlLabel>
                  <Select options={this._getFormattedStreams(this.props.streams)}
                          onChange={this._onStreamSelect}
                          value={this.state.stream.id}
                          required
                          clearable={false} />
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
  }
}

export default ProcessorSimulator;
