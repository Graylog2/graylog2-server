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
import lodash from 'lodash';
import styled from 'styled-components';

import { CounterDetails, GaugeDetails, HistogramDetails, MeterDetails, TimerDetails } from 'components/metrics';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

const { MetricsStore, MetricsActions } = CombinedProvider.get('Metrics');
const StyledMetricDetail = styled.div`
  dl {
    > dt {
      float: left;
    }

    &.metric-timer > dd {
      margin-left: 145px;
    }

    &.metric-meter > dd {
      margin-left: 115px;
    }

    &.metric-gauge > dd {
      margin-left: 90px;
    }

    &.metric-counter > dd {
      margin-left: 90px;
    }

    &.metric-histogram > dd {
      margin-left: 145px;
    }
  }
`;

class MetricDetails extends React.Component {
 static propTypes = {
   metrics: PropTypes.object,
   metric: PropTypes.object.isRequired,
   nodeId: PropTypes.string.isRequired,
 };

 static defaultProps = {
   metrics: undefined,
 }

 componentDidMount() {
   const { nodeId, metric: { full_name: fullName } } = this.props;
   MetricsActions.add(nodeId, fullName);
 }

 componentWillUnmount() {
   const { nodeId, metric: { full_name: fullName } } = this.props;
   MetricsActions.remove(nodeId, fullName);
 }

 _formatDetailsForType = (type, metric) => {
   switch (type) {
     case 'Counter':
       return <CounterDetails metric={metric} />;
     case 'Gauge':
       return <GaugeDetails metric={metric} />;
     case 'Histogram':
       return <HistogramDetails metric={metric} />;
     case 'Meter':
       return <MeterDetails metric={metric} />;
     case 'Timer':
       return <TimerDetails metric={metric} />;
     default:
       return <i>Invalid metric type: {type}</i>;
   }
 }

 render() {
   const { nodeId, metric, metric: { full_name: metricName }, metrics } = this.props;
   const currentMetric = metrics?.[nodeId]?.[metricName] ?? metric;
   const type = lodash.capitalize(currentMetric.type);
   const details = this._formatDetailsForType(type, currentMetric);

   return (
     <div className="metric">
       <h3>{type}</h3>
       <StyledMetricDetail>
         {details}
       </StyledMetricDetail>
     </div>
   );
 }
}

export default connect(
  MetricDetails,
  { metricsStore: MetricsStore },
  ({ metricsStore, ...otherProps }) => ({
    ...otherProps,
    metrics: metricsStore.metrics,
  }),
);
