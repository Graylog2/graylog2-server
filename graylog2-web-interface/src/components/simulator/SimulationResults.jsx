/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import PropTypes from 'prop-types';

import { Alert, Col, DropdownButton, MenuItem, Row } from 'components/graylog';
import { Spinner } from 'components/common';
import MessageShow from 'components/search/MessageShow';
import NumberUtils from 'util/NumberUtils';

import SimulationChanges from './SimulationChanges';
import SimulationPreview from './SimulationPreview';
import SimulationTrace from './SimulationTrace';

// eslint-disable-next-line import/no-webpack-loader-syntax
import style from '!style/useable!css!./SimulationResults.css';

const VIEW_OPTIONS = {
  SIMULATION_PREVIEW: 1,
  SIMULATION_SUMMARY: 2,
  SIMULATION_TRACE: 3,
};

class SimulationResults extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      viewOption: VIEW_OPTIONS.SIMULATION_SUMMARY,
    };
  }

  componentDidMount() {
    style.use();
  }

  componentWillUnmount() {
    style.unuse();
  }

  _changeViewOptions = (eventKey) => {
    const selectedOption = Object.keys(VIEW_OPTIONS).find((key) => VIEW_OPTIONS[key] === eventKey);

    this.setState({ viewOption: VIEW_OPTIONS[selectedOption] });
  };

  _getViewOptionsMenuItems = () => [
    this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_SUMMARY, 'Changes summary'),
    this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_PREVIEW, 'Results preview'),
    this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_TRACE, 'Simulation trace'),
  ];

  _getViewOptionsMenuItem = (option, text) => {
    const { viewOption } = this.state;

    return (
      <MenuItem key={option} eventKey={option} active={viewOption === option}>
        {text}
      </MenuItem>
    );
  };

  _getViewComponent = (streams) => {
    const { simulationResults, isLoading, originalMessage } = this.props;

    if (isLoading || !simulationResults) {
      return <Spinner />;
    }

    const { viewOption } = this.state;

    switch (viewOption) {
      case VIEW_OPTIONS.SIMULATION_PREVIEW:
        return <SimulationPreview simulationResults={simulationResults} streams={streams} />;
      case VIEW_OPTIONS.SIMULATION_SUMMARY:
        return (
          <SimulationChanges originalMessage={originalMessage}
                             simulationResults={simulationResults} />
        );
      case VIEW_OPTIONS.SIMULATION_TRACE:
        return <SimulationTrace simulationResults={simulationResults} />;
      default:
      // it should never happen™
    }

    return null;
  };

  render() {
    const { stream, simulationResults, isLoading, error, originalMessage } = this.props;

    if (!originalMessage && !simulationResults) {
      return null;
    }

    const streams = {
      [stream.id]: stream,
    };

    // eslint-disable-next-line no-nested-ternary
    const originalMessagePreview = isLoading
      ? <Spinner />
      : originalMessage
        ? (
          <MessageShow message={originalMessage}
                       streams={streams} />
        )
        : null;

    const errorMessage = error
      ? (
        <Alert bsStyle="danger">
          <p><strong>Error simulating message processing</strong></p>
          <p>
            Could not simulate processing of message <em>{originalMessage.id}</em> in stream{' '}
            <em>{stream.title}</em>.
            <br />
            Please try loading the message again, or use another message for the simulation.
          </p>
        </Alert>
      )
      : null;

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
            {isLoading
              ? 'Simulating message processing, please wait a moment.'
              : `These are the results of processing the loaded message. Processing took ${NumberUtils.formatNumber(simulationResults.took_microseconds)} µs.`}
          </p>
          {errorMessage}
          {this._getViewComponent(streams)}
        </Col>
      </Row>
    );
  }
}

SimulationResults.propTypes = {
  stream: PropTypes.object.isRequired,
  originalMessage: PropTypes.object.isRequired,
  simulationResults: PropTypes.object,
  isLoading: PropTypes.bool,
  error: PropTypes.object,
};

SimulationResults.defaultProps = {
  simulationResults: undefined,
  isLoading: false,
  error: undefined,
};

export default SimulationResults;
