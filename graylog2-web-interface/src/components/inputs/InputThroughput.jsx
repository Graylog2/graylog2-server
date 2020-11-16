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
/* eslint-disable no-restricted-globals */
import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import numeral from 'numeral';
import styled, { css } from 'styled-components';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';
import NumberUtils from 'util/NumberUtils';
import { Icon, LinkToNode, Spinner } from 'components/common';

const InputIO = styled.span(({ theme }) => css`
  .total {
    color: ${theme.colors.gray[70]};
  }

  .value {
    font-family: ${theme.fonts.family.monospace};
  }

  .persec {
    margin-left: 3px;
  }

  .channel-direction {
    position: relative;
    left: -1px;
  }

  .channel-direction-down {
    position: relative;
    top: 1px;
  }

  .channel-direction-up {
    position: relative;
    top: -1px;
  }
`);

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const InputThroughput = createReactClass({
  displayName: 'InputThroughput',

  propTypes: {
    input: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  getInitialState() {
    return {
      showDetails: false,
    };
  },

  UNSAFE_componentWillMount() {
    this._metricNames().forEach((metricName) => MetricsActions.addGlobal(metricName));
  },

  componentWillUnmount() {
    this._metricNames().forEach((metricName) => MetricsActions.removeGlobal(metricName));
  },

  _metricNames() {
    return [
      this._prefix('incomingMessages'),
      this._prefix('emptyMessages'),
      this._prefix('open_connections'),
      this._prefix('total_connections'),
      this._prefix('written_bytes_1sec'),
      this._prefix('written_bytes_total'),
      this._prefix('read_bytes_1sec'),
      this._prefix('read_bytes_total'),
    ];
  },

  _prefix(metric) {
    const { input } = this.props;

    return `${input.type}.${input.id}.${metric}`;
  },

  _getValueFromMetric(metric) {
    if (metric === null || metric === undefined) {
      return undefined;
    }

    switch (metric.type) {
      case 'meter':
        return metric.metric.rate.mean;
      case 'gauge':
        return metric.metric.value;
      case 'counter':
        return metric.metric.count;
      default:
        return undefined;
    }
  },

  _calculateMetrics(metrics) {
    const result = {};

    this._metricNames().forEach((metricName) => {
      result[metricName] = Object.keys(metrics).reduce((previous, nodeId) => {
        if (!metrics[nodeId][metricName]) {
          return previous;
        }

        const value = this._getValueFromMetric(metrics[nodeId][metricName]);

        if (value !== undefined) {
          return isNaN(previous) ? value : previous + value;
        }

        return previous;
      }, NaN);
    });

    return result;
  },

  _formatCount(count) {
    return numeral(count).format('0,0');
  },

  _formatNetworkStats(writtenBytes1Sec, writtenBytesTotal, readBytes1Sec, readBytesTotal) {
    const network = (
      <InputIO>
        <span>Network IO: </span>
        <span className="persec">
          <Icon name="caret-down" className="channel-direction channel-direction-down" />
          <span className="rx value">{NumberUtils.formatBytes(readBytes1Sec)} </span>

          <Icon name="caret-up" className="channel-direction channel-direction-up" />
          <span className="tx value">{NumberUtils.formatBytes(writtenBytes1Sec)}</span>
        </span>

        <span className="total">
          <span> (total: </span>
          <Icon name="caret-down" className="channel-direction channel-direction-down" />
          <span className="rx value">{NumberUtils.formatBytes(readBytesTotal)} </span>

          <Icon name="caret-up" className="channel-direction channel-direction-up" />
          <span className="tx value">{NumberUtils.formatBytes(writtenBytesTotal)}</span>
          <span> )</span>
        </span>
        <br />
      </InputIO>
    );

    return network;
  },

  _formatConnections(openConnections, totalConnections) {
    return (
      <span>
        Active connections: <span className="active">{this._formatCount(openConnections)} </span>
        (<span className="total">{this._formatCount(totalConnections)}</span> total)
        <br />
      </span>
    );
  },

  _formatAllNodeDetails(metrics) {
    return (
      <span>
        <hr key="separator" />
        {Object.keys(metrics).map((nodeId) => this._formatNodeDetails(nodeId, metrics[nodeId]))}
      </span>
    );
  },

  _formatNodeDetails(nodeId, metrics) {
    const { input } = this.props;
    const openConnections = this._getValueFromMetric(metrics[this._prefix('open_connections')]);
    const totalConnections = this._getValueFromMetric(metrics[this._prefix('total_connections')]);
    const emptyMessages = this._getValueFromMetric(metrics[this._prefix('emptyMessages')]);
    const writtenBytes1Sec = this._getValueFromMetric(metrics[this._prefix('written_bytes_1sec')]);
    const writtenBytesTotal = this._getValueFromMetric(metrics[this._prefix('written_bytes_total')]);
    const readBytes1Sec = this._getValueFromMetric(metrics[this._prefix('read_bytes_1sec')]);
    const readBytesTotal = this._getValueFromMetric(metrics[this._prefix('read_bytes_total')]);

    return (
      <span key={input.id + nodeId}>
        <LinkToNode nodeId={nodeId} />
        <br />
        {!isNaN(writtenBytes1Sec) && this._formatNetworkStats(writtenBytes1Sec, writtenBytesTotal, readBytes1Sec, readBytesTotal)}
        {!isNaN(openConnections) && this._formatConnections(openConnections, totalConnections)}
        {!isNaN(emptyMessages) && <span>Empty messages discarded: {this._formatCount(emptyMessages)}<br /></span>}
        {isNaN(writtenBytes1Sec) && isNaN(openConnections) && <span>No metrics available for this node</span>}
        <br />
      </span>
    );
  },

  _toggleShowDetails(evt) {
    evt.preventDefault();
    const { showDetails } = this.state;

    this.setState({ showDetails: !showDetails });
  },

  render() {
    const { metrics, showDetails } = this.state;
    const { input } = this.props;

    if (!metrics) {
      return <Spinner />;
    }

    const calculatedMetrics = this._calculateMetrics(metrics);
    const incomingMessages = calculatedMetrics[this._prefix('incomingMessages')];
    const emptyMessages = calculatedMetrics[this._prefix('emptyMessages')];
    const openConnections = calculatedMetrics[this._prefix('open_connections')];
    const totalConnections = calculatedMetrics[this._prefix('total_connections')];
    const writtenBytes1Sec = calculatedMetrics[this._prefix('written_bytes_1sec')];
    const writtenBytesTotal = calculatedMetrics[this._prefix('written_bytes_total')];
    const readBytes1Sec = calculatedMetrics[this._prefix('read_bytes_1sec')];
    const readBytesTotal = calculatedMetrics[this._prefix('read_bytes_total')];

    return (
      <div className="graylog-input-metrics">
        <h3>Throughput / Metrics</h3>
        <span>
          {isNaN(incomingMessages) && isNaN(writtenBytes1Sec) && isNaN(openConnections) && <i>No metrics available for this input</i>}
          {!isNaN(incomingMessages) && <span>1 minute average rate: {this._formatCount(incomingMessages)} msg/s<br /></span>}
          {!isNaN(writtenBytes1Sec) && this._formatNetworkStats(writtenBytes1Sec, writtenBytesTotal, readBytes1Sec, readBytesTotal)}
          {!isNaN(openConnections) && this._formatConnections(openConnections, totalConnections)}
          {!isNaN(emptyMessages) && <span>Empty messages discarded: {this._formatCount(emptyMessages)}<br /></span>}
          { /* eslint-disable-next-line jsx-a11y/anchor-is-valid */ }
          {!isNaN(writtenBytes1Sec) && input.global && <a href="" onClick={this._toggleShowDetails}>{showDetails ? 'Hide' : 'Show'} details</a>}
          {!isNaN(writtenBytes1Sec) && showDetails && this._formatAllNodeDetails(metrics)}
        </span>
      </div>
    );
  },
});

export default InputThroughput;
