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
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
import ActionsProvider from 'injection/ActionsProvider';

const MetricsStore = StoreProvider.getStore('Metrics');
const MetricsActions = ActionsProvider.getActions('Metrics');

const GlobalThroughputStore = Reflux.createStore({
  listenables: [],
  throughput: {
    input: 0,
    output: 0,
    loading: false,
  },
  metrics: {
    input: 'org.graylog2.throughput.input.1-sec-rate',
    output: 'org.graylog2.throughput.output.1-sec-rate',
    loading: true,
  },

  init() {
    MetricsActions.addGlobal(this.metrics.input);
    MetricsActions.addGlobal(this.metrics.output);
    this.listenTo(MetricsStore, this.updateMetrics);
    setInterval(MetricsActions.list, this.INTERVAL);
  },

  getInitialState() {
    return { throughput: this.throughput };
  },

  INTERVAL: 2000,
  updateMetrics(update) {
    if (!update.metrics) {
      return;
    }

    const input = Object.keys(update.metrics)
      .map((nodeId) => update.metrics[nodeId][this.metrics.input]?.metric?.value ?? 0)
      .reduce((prev, cur) => prev + cur, 0);
    const output = Object.keys(update.metrics)
      .map((nodeId) => update.metrics[nodeId][this.metrics.output]?.metric?.value ?? 0)
      .reduce((prev, cur) => prev + cur, 0);

    this.throughput = { input, output, loading: false };

    this.trigger({ throughput: this.throughput });
  },
});

export default GlobalThroughputStore;
