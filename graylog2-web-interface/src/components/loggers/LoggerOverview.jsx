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
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Spinner } from 'components/common';
import { NodeLoggers } from 'components/loggers';
import StoreProvider from 'injection/StoreProvider';

const LoggersStore = StoreProvider.getStore('Loggers');

const LoggerOverview = createReactClass({
  displayName: 'LoggerOverview',
  mixins: [Reflux.connect(LoggersStore)],

  render() {
    if (!this.state.loggers || !this.state.subsystems) {
      return <Spinner />;
    }

    const { subsystems } = this.state;
    const nodeLoggers = Object.keys(this.state.loggers)
      .map((nodeId) => (
        <NodeLoggers key={`node-loggers-${nodeId}`}
                     nodeId={nodeId}
                     subsystems={subsystems[nodeId] ? subsystems[nodeId].subsystems : {}} />
      ));

    return (
      <span>
        {nodeLoggers}
      </span>
    );
  },
});

export default LoggerOverview;
