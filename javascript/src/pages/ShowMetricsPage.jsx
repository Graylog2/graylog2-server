import React from 'react';
import Reflux from 'reflux';

import MetricsStore from 'stores/metrics/MetricsStore';

import MetricsActions from 'actions/metrics/MetricsActions';

import { PageHeader, Spinner } from 'components/common';
import { MetricsComponent } from 'components/metrics';

const ShowMetricsPage = React.createClass({
  mixins: [Reflux.connect(MetricsStore)],

  componentDidMount() {
    setInterval(MetricsActions.list, 2000);
  },
  render() {
    return (
      <span>
        <PageHeader title="Metrics">
          <span>
            All Graylog nodes provide a set of internal metrics for diagnosis, debugging and monitoring. Note that you can access
            all metrics via JMX, too.
          </span>
          <span>This node is reporting a total of {this.state.names ? this.state.names.length : <Spinner />} metrics.</span>
        </PageHeader>

        <MetricsComponent />
      </span>
    );
  },
});

export default ShowMetricsPage;
