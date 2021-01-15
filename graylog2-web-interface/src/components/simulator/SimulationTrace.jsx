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

  style: require('!style/useable!css!./SimulationTrace.css'),

  render() {
    const simulationTrace = this.props.simulationResults.simulation_trace;

    const traceEntries = [];

    simulationTrace.forEach((trace, idx) => {
      traceEntries.push(<dt key={`${trace.time}-${idx}-title`}>{NumberUtils.formatNumber(trace.time)} &#956;s</dt>);
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
