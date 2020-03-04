import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Alert, Col, DropdownButton, MenuItem, Row } from 'components/graylog';

import { Spinner } from 'components/common';
import MessageShow from 'components/search/MessageShow';

import NumberUtils from 'util/NumberUtils';
import SimulationChanges from './SimulationChanges';
import SimulationPreview from './SimulationPreview';
import SimulationTrace from './SimulationTrace';


const SimulationResults = createReactClass({
  displayName: 'SimulationResults',

  propTypes: {
    stream: PropTypes.object.isRequired,
    originalMessage: PropTypes.object,
    simulationResults: PropTypes.object,
    isLoading: PropTypes.bool,
    error: PropTypes.object,
  },

  getInitialState() {
    return {
      viewOption: this.VIEW_OPTIONS.SIMULATION_SUMMARY,
    };
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  VIEW_OPTIONS: {
    SIMULATION_PREVIEW: 1,
    SIMULATION_SUMMARY: 2,
    SIMULATION_TRACE: 3,
  },

  style: require('!style/useable!css!./SimulationResults.css'),

  _changeViewOptions(eventKey) {
    const selectedOption = Object.keys(this.VIEW_OPTIONS).find(key => this.VIEW_OPTIONS[key] === eventKey);
    this.setState({ viewOption: this.VIEW_OPTIONS[selectedOption] });
  },

  _getViewOptionsMenuItems() {
    const viewOptionsMenuItems = [];

    viewOptionsMenuItems.push(this._getViewOptionsMenuItem(this.VIEW_OPTIONS.SIMULATION_SUMMARY, 'Changes summary'));
    viewOptionsMenuItems.push(this._getViewOptionsMenuItem(this.VIEW_OPTIONS.SIMULATION_PREVIEW, 'Results preview'));
    viewOptionsMenuItems.push(this._getViewOptionsMenuItem(this.VIEW_OPTIONS.SIMULATION_TRACE, 'Simulation trace'));

    return viewOptionsMenuItems;
  },

  _getViewOptionsMenuItem(option, text) {
    return (
      <MenuItem key={option} eventKey={option} active={this.state.viewOption === option}>
        {text}
      </MenuItem>
    );
  },

  _getViewComponent(streams) {
    if (this.props.isLoading || !this.props.simulationResults) {
      return <Spinner />;
    }

    switch (this.state.viewOption) {
      case this.VIEW_OPTIONS.SIMULATION_PREVIEW:
        return <SimulationPreview simulationResults={this.props.simulationResults} streams={streams} />;
      case this.VIEW_OPTIONS.SIMULATION_SUMMARY:
        return <SimulationChanges originalMessage={this.props.originalMessage} simulationResults={this.props.simulationResults} />;
      case this.VIEW_OPTIONS.SIMULATION_TRACE:
        return <SimulationTrace simulationResults={this.props.simulationResults} />;
      default:
      // it should never happen™
    }

    return null;
  },

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
                     streams={streams} />
      );
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
          <div className="pull-right">
            <DropdownButton id="simulation-view-options"
                            title="More results"
                            onSelect={this._changeViewOptions}
                            bsStyle="default"
                            bsSize="small"
                            pullRight>
              {this._getViewOptionsMenuItems()}
            </DropdownButton>
          </div>
          <h1>Simulation results</h1>
          <p>
            {this.props.isLoading
              ? 'Simulating message processing, please wait a moment.'
              : `These are the results of processing the loaded message. Processing took ${NumberUtils.formatNumber(this.props.simulationResults.took_microseconds)} µs.`}
          </p>
          {errorMessage}
          {this._getViewComponent(streams)}
        </Col>
      </Row>
    );
  },
});

export default SimulationResults;
