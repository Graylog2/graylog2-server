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
import * as Immutable from 'immutable';
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Alert, Col, DropdownButton, MenuItem, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import MessageShow from 'components/search/MessageShow';
import NumberUtils from 'util/NumberUtils';
import type { Stream } from 'logic/streams/types';

import SimulationChanges from './SimulationChanges';
import SimulationPreview from './SimulationPreview';
import SimulationTrace from './SimulationTrace';
import style from './SimulationResults.lazy.css';

const VIEW_OPTIONS = {
  SIMULATION_PREVIEW: 1,
  SIMULATION_SUMMARY: 2,
  SIMULATION_TRACE: 3,
};

type SimulationResultsProps = {
  stream: Stream;
  originalMessage?: any;
  simulationResults?: any;
  isLoading?: boolean;
  error?: any;
} & WithTranslation;

class SimulationResults extends React.Component<
  SimulationResultsProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    originalMessage: undefined,
    simulationResults: undefined,
    isLoading: false,
    error: undefined,
  };

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

  _getViewOptionsMenuItems = () => {
    const { t } = this.props;

    return [
      this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_SUMMARY, t('simulation_results.changes_summary')),
      this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_PREVIEW, t('simulation_results.results_preview')),
      this._getViewOptionsMenuItem(VIEW_OPTIONS.SIMULATION_TRACE, t('simulation_results.simulation_trace')),
    ];
  };

  _getViewOptionsMenuItem = (option, text) => {
    const { viewOption } = this.state;

    return (
      <MenuItem key={option} onSelect={() => this._changeViewOptions(option)} active={viewOption === option}>
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
        return <SimulationChanges originalMessage={originalMessage} simulationResults={simulationResults} />;
      case VIEW_OPTIONS.SIMULATION_TRACE:
        return <SimulationTrace simulationResults={simulationResults} />;
      default:
      // it should never happen™
    }

    return null;
  };

  render() {
    const { stream, simulationResults, isLoading, error, originalMessage, t } = this.props;

    if (!originalMessage && !simulationResults) {
      return null;
    }

    const streams = Immutable.Map({
      [stream.id]: stream,
    });

    // eslint-disable-next-line no-nested-ternary
    const originalMessagePreview = isLoading ? (
      <Spinner />
    ) : originalMessage ? (
      <MessageShow message={originalMessage} streams={streams} />
    ) : null;

    const errorMessage = error ? (
      <Alert bsStyle="danger" title={t('simulation_results.error_title')}>
        <p>
          <Trans
            t={t}
            i18nKey="simulation_results.error"
            values={{ id: originalMessage.id, stream: stream.title }}
            components={{ emphasis: <em />, lineBreak: <br /> }}
          />
        </p>
      </Alert>
    ) : null;

    return (
      <Row>
        <Col md={12}>
          <hr />
        </Col>
        <Col md={6}>
          <h1>{t('simulation_results.original_message')}</h1>
          <div className="message-preview-wrapper">{originalMessagePreview}</div>
        </Col>
        <Col md={6}>
          <div className="pull-right">
            <DropdownButton
              id="simulation-view-options"
              title={t('simulation_results.more_results')}
              bsStyle="default"
              bsSize="small"
              pullRight>
              {this._getViewOptionsMenuItems()}
            </DropdownButton>
          </div>
          <h1>{t('simulation_results.title')}</h1>
          <p>
            {isLoading
              ? t('simulation_results.simulating')
              : t('simulation_results.results_description', {
                  took: NumberUtils.formatNumber(simulationResults?.took_microseconds),
                })}
          </p>
          {errorMessage}
          {this._getViewComponent(streams)}
        </Col>
      </Row>
    );
  }
}

export default withTranslation('pipelines')(SimulationResults);
