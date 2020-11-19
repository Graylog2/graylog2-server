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

import { MetricDetails } from 'components/metrics';
import { Icon } from 'components/common';

const Metric = createReactClass({
  displayName: 'Metric',

  propTypes: {
    metric: PropTypes.object.isRequired,
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
  },

  getInitialState() {
    return {
      expanded: false,
    };
  },

  iconMapping: {
    timer: 'clock',
    histogram: 'signal',
    meter: 'play-circle',
    gauge: 'tachometer-alt',
    counter: 'circle',
    unknown: 'question-circle',
  },

  _formatIcon(type) {
    const icon = this.iconMapping[type];

    if (icon) {
      return icon;
    }

    return this.iconMapping.unknown;
  },

  _formatName(metricName) {
    const { namespace } = this.props;
    const split = metricName.split(namespace);
    const unqualifiedMetricName = split.slice(1).join(namespace);

    return (
      <span>
        <span className="prefix">{namespace}</span>
        {unqualifiedMetricName}
      </span>
    );
  },

  _showDetails(event) {
    event.preventDefault();
    this.setState({ expanded: !this.state.expanded });
  },

  render() {
    const { metric } = this.props;
    const details = this.state.expanded ? <MetricDetails nodeId={this.props.nodeId} metric={this.props.metric} /> : null;

    return (
      <span>
        <div className="name">
          <Icon name={this._formatIcon(metric.type)} />{' '}
          <a className="open" href="#" onClick={this._showDetails}>{this._formatName(metric.full_name)}</a>
        </div>
        {details}
      </span>
    );
  },
});

export default Metric;
