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

import StoreProvider from 'injection/StoreProvider';
import { LogLevelMetrics } from 'components/loggers';

const LoggersStore = StoreProvider.getStore('Loggers');

const LogLevelMetricsOverview = createReactClass({
  displayName: 'LogLevelMetricsOverview',

  propTypes: {
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(LoggersStore)],

  render() {
    const { nodeId } = this.props;
    const logLevelMetrics = this.state.availableLoglevels
      .map((loglevel) => <LogLevelMetrics key={`loglevel-metrics-${nodeId}-${loglevel}`} nodeId={nodeId} loglevel={loglevel} />);

    return (
      <div className="loglevel-metrics">
        {logLevelMetrics}
      </div>
    );
  },
});

export default LogLevelMetricsOverview;
