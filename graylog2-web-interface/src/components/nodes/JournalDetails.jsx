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
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Link } from 'components/graylog/router';

import numeral from 'numeral';
import moment from 'moment';
import {} from 'moment-duration-format';
import styled from 'styled-components';

import { Row, Col, Alert } from 'components/graylog';
import ProgressBar, { Bar } from 'components/graylog/ProgressBar';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import ActionsProvider from 'injection/ActionsProvider';
import StoreProvider from 'injection/StoreProvider';
import { Spinner, Timestamp, Icon } from 'components/common';
import NumberUtils from 'util/NumberUtils';
import Routes from 'routing/Routes';

const MetricsActions = ActionsProvider.getActions('Metrics');
const MetricsStore = StoreProvider.getStore('Metrics');
const JournalStore = StoreProvider.getStore('Journal');

const JournalUsageProgressBar = styled(ProgressBar)`
  margin-bottom: 5px;
  margin-top: 10px;

  ${Bar} {
    min-width: 3em;
  }
`;

const JournalDetails = createReactClass({
  displayName: 'JournalDetails',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getInitialState() {
    return {
      journalInformation: undefined,
    };
  },

  componentDidMount() {
    const { nodeId } = this.props;

    JournalStore.get(nodeId).then((journalInformation) => {
      this.setState({ journalInformation: journalInformation }, this._listenToMetrics);
    });
  },

  componentWillUnmount() {
    const { nodeId } = this.props;

    if (this.metricNames) {
      Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.remove(nodeId, this.metricNames[metricShortName]));
    }
  },

  _listenToMetrics() {
    const { nodeId } = this.props;
    const { journalInformation } = this.state;

    // only listen for updates if the journal is actually turned on
    if (journalInformation.enabled) {
      this.metricNames = {
        append: 'org.graylog2.journal.append.1-sec-rate',
        read: 'org.graylog2.journal.read.1-sec-rate',
        segments: 'org.graylog2.journal.segments',
        entriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
        utilizationRatio: 'org.graylog2.journal.utilization-ratio',
        oldestSegment: 'org.graylog2.journal.oldest-segment',
      };

      Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.add(nodeId, this.metricNames[metricShortName]));
    }
  },

  _isLoading() {
    const { journalInformation, metrics } = this.state;

    return !(metrics && journalInformation);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner text="Loading journal metrics..." />;
    }

    const { nodeId } = this.props;
    const { metrics: metricsState } = this.state;
    const nodeMetrics = metricsState[nodeId];
    const { journalInformation } = this.state;

    if (!journalInformation.enabled) {
      return (
        <Alert bsStyle="warning">
          <Icon name="exclamation-triangle" />&nbsp; The disk journal is disabled on this node.
        </Alert>
      );
    }

    const metrics = this.metricNames ? MetricsExtractor.getValuesForNode(nodeMetrics, this.metricNames) : {};

    if (Object.keys(metrics).length === 0) {
      return (
        <Alert bsStyle="warning">
          <Icon name="exclamation-triangle" />&nbsp; Journal metrics unavailable.
        </Alert>
      );
    }

    const oldestSegment = moment(metrics.oldestSegment);
    let overcommittedWarning;

    if (metrics.utilizationRatio >= 1) {
      overcommittedWarning = (
        <span>
          <strong>Warning!</strong> The journal utilization is exceeding the maximum size defined.
          {' '}<Link to={Routes.SYSTEM.OVERVIEW}>Click here</Link> for more information.<br />
        </span>
      );
    }

    return (
      <Row className="row-sm">
        <Col md={6}>
          <h3>Configuration</h3>
          <dl className="system-journal">
            <dt>Path:</dt>
            <dd>{journalInformation.journal_config.directory}</dd>
            <dt>Earliest entry:</dt>
            <dd><Timestamp dateTime={oldestSegment} relative /></dd>
            <dt>Maximum size:</dt>
            <dd>{NumberUtils.formatBytes(journalInformation.journal_config.max_size)}</dd>
            <dt>Maximum age:</dt>
            <dd>{moment.duration(journalInformation.journal_config.max_age).format('d [days] h [hours] m [minutes]')}</dd>
            <dt>Flush policy:</dt>
            <dd>
              Every {numeral(journalInformation.journal_config.flush_interval).format('0,0')} messages
              {' '}or {moment.duration(journalInformation.journal_config.flush_age).format('h [hours] m [minutes] s [seconds]')}
            </dd>
          </dl>
        </Col>
        <Col md={6}>
          <h3>Utilization</h3>

          <JournalUsageProgressBar bars={[{
            value: metrics.utilizationRatio * 100,
            label: NumberUtils.formatPercentage(metrics.utilizationRatio),
          }]} />

          {overcommittedWarning}

          <strong>{numeral(metrics.entriesUncommitted).format('0,0')} unprocessed messages</strong>
          {' '}are currently in the journal, in {metrics.segments} segments.<br />
          <strong>{numeral(metrics.append).format('0,0')} messages</strong>
          {' '}have been appended in the last second,{' '}
          <strong>{numeral(metrics.read).format('0,0')} messages</strong> have been read in the last second.
        </Col>
      </Row>
    );
  },
});

export default JournalDetails;
