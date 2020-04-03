// @flow strict
import PropTypes from 'prop-types';
import * as React from 'react';

import TimeHelper from 'util/TimeHelper';

import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';

import type { ClusterMetric } from 'stores/metrics/MetricsStore';

const { MetricsStore, MetricsActions } = CombinedProvider.get('Metrics');

type Props = {
  metrics: ClusterMetric,
  metricsUpdatedAt: number,
  name: string,
  zeroOnMissing: boolean,
  children: React.Node,
};

class MetricContainer extends React.Component<Props> {
  static propTypes = {
    metrics: PropTypes.shape({
      nodeId: PropTypes.string,
      nodeMetrics: PropTypes.shape({
        metricName: PropTypes.string,
        metricData: PropTypes.shape({
          type: PropTypes.oneOf(['gauge', 'counter', 'meter', 'timer']),
          full_name: PropTypes.string,
          metric: PropTypes.object,
          name: PropTypes.string,
        }),
      }),
    }),
    metricsUpdatedAt: PropTypes.number,
    name: PropTypes.string.isRequired,
    zeroOnMissing: PropTypes.bool,
    children: PropTypes.node.isRequired,
  };

  static defaultProps = {
    metrics: {},
    metricsUpdatedAt: TimeHelper.nowInSeconds(),
    zeroOnMissing: true,
  };

  componentDidMount() {
    const { name } = this.props;
    MetricsActions.addGlobal(name);
  }

  shouldComponentUpdate(nextProps) {
    // Do not render this component and it's children when no metric data has changed.
    // This component and the CounterRate component expect to be rendered every second or less often. When using
    // these components on a page that triggers a re-render more often - e.g. by having another setInterval - the
    // calculation in CounterRate will break.
    const { metricsUpdatedAt } = this.props;
    if (metricsUpdatedAt !== null && nextProps.metricsUpdatedAt) {
      return nextProps.metricsUpdatedAt > metricsUpdatedAt;
    }
    return true;
  }

  componentWillUnmount() {
    const { name } = this.props;
    MetricsActions.removeGlobal(name);
  }

  render() {
    const { children, metrics, name: fullName, zeroOnMissing } = this.props;
    if (!metrics) {
      return (<span>Loading...</span>);
    }
    let throughput = Object.keys(metrics)
      .map((nodeId) => MetricsExtractor.getValuesForNode(metrics[nodeId], { throughput: fullName }))
      .reduce((accumulator: { throughput?: number }, currentMetric: { throughput: ?number }): { throughput?: number } => {
        return { throughput: (accumulator.throughput || 0) + (currentMetric.throughput || 0) };
      }, {});
    if (zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput = { throughput: 0 };
    }
    return (
      <div>
        {
        React.Children.map(children, (child) => {
          return React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput } });
        })
      }
      </div>
    );
  }
}

export default connect(MetricContainer,
  { metricsStore: MetricsStore },
  ({ metricsStore, ...otherProps }) => ({
    ...otherProps,
    metrics: metricsStore.metrics,
    metricsUpdatedAt: metricsStore.metricsUpdatedAt,
  }));
