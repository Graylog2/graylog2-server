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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import numeral from 'numeral';

import { Pluralize, Spinner } from 'components/common';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const JournalState = createReactClass({
  displayName: 'JournalState',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(MetricsStore)],

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillMount() {
    this.metricNames = {
      append: 'org.graylog2.journal.append.1-sec-rate',
      read: 'org.graylog2.journal.read.1-sec-rate',
      segments: 'org.graylog2.journal.segments',
      entriesUncommitted: 'org.graylog2.journal.entries-uncommitted',
    };

    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.add(this.props.nodeId, this.metricNames[metricShortName]));
  },

  componentWillUnmount() {
    Object.keys(this.metricNames).forEach((metricShortName) => MetricsActions.remove(this.props.nodeId, this.metricNames[metricShortName]));
  },

  _isLoading() {
    return !this.state.metrics;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner text="Loading journal metrics..." />;
    }

    const { nodeId } = this.props;
    const nodeMetrics = this.state.metrics[nodeId];
    const metrics = MetricsExtractor.getValuesForNode(nodeMetrics, this.metricNames);

    if (Object.keys(metrics).length === 0) {
      return <span>Journal metrics unavailable.</span>;
    }

    return (
      <span>
        The journal contains <strong>{numeral(metrics.entriesUncommitted).format('0,0')} unprocessed messages</strong> in {metrics.segments}
        {' '}<Pluralize value={metrics.segments} singular="segment" plural="segments" />.{' '}
        <strong>{numeral(metrics.append).format('0,0')} messages</strong> appended, <strong>
          {numeral(metrics.read).format('0,0')} messages
        </strong> read in the last second.
      </span>
    );
  },
});

export default JournalState;
