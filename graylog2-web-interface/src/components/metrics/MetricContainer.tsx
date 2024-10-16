import * as React from 'react';

import TimeHelper from 'util/TimeHelper';
import connect from 'stores/connect';
import MetricsExtractor from 'logic/metrics/MetricsExtractor';
import type { ClusterMetric } from 'stores/metrics/MetricsStore';
import type { Store } from 'stores/StoreTypes';
import { MetricsActions, MetricsStore } from 'stores/metrics/MetricsStore';

type Props = {
  metrics: ClusterMetric,
  metricsUpdatedAt: number,
  name: string,
  zeroOnMissing: boolean,
  children: React.ReactElement[] | React.ReactElement,
};

class MetricContainer extends React.Component<Props> {
  static defaultProps = {
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
      .reduce((accumulator: { throughput?: number }, currentMetric: { throughput: number | undefined | null }): { throughput?: number } => ({ throughput: (accumulator.throughput || 0) + (currentMetric.throughput || 0) }), {});

    if (zeroOnMissing && (!throughput || !throughput.throughput)) {
      throughput = { throughput: 0 };
    }

    return (
      <div>
        {
        React.Children.map(children, (child) => React.cloneElement(child, { metric: { full_name: fullName, count: throughput.throughput } }))
      }
      </div>
    );
  }
}

type MetricsStoreState = {
  metrics: ClusterMetric,
  metricsUpdatedAt: number,
};

export default connect(MetricContainer,
  { metricsStore: MetricsStore as Store<MetricsStoreState> },
  ({ metricsStore, ...otherProps }) => ({
    ...otherProps,
    metrics: metricsStore.metrics,
    metricsUpdatedAt: metricsStore.metricsUpdatedAt,
  }));
