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

import NumberUtils from 'util/NumberUtils';

const SimulationTrace = createReactClass({
  displayName: 'SimulationTrace',

  propTypes: {
    simulationResults: PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.style.use();
  },

  componentWillUnmount() {
    this.style.unuse();
  },

  // eslint-disable-next-line global-require
  style: require('./SimulationTrace.lazy.css'),

  render() {
    const { simulationResults } = this.props;

    const simulationTrace = simulationResults.simulation_trace;

    const traceEntries = [];

    simulationTrace.forEach((trace, idx) => {
      // eslint-disable-next-line react/no-array-index-key
      traceEntries.push(<dt key={`${trace.time}-${idx}-title`}>{NumberUtils.formatNumber(trace.time)} &#956;s</dt>);
      // eslint-disable-next-line react/no-array-index-key
      traceEntries.push(<dd key={`${trace}-${idx}-description`}><span>{trace.message}</span></dd>);
    });

    return (
      <dl className="dl-horizontal dl-simulation-trace">
        {traceEntries}
      </dl>
    );
  },
});

export default SimulationTrace;
