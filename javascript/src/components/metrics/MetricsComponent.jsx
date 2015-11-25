import React from 'react';
import Reflux from 'reflux';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Col, Row } from 'react-bootstrap';
import String from 'string';

import MetricsStore from 'stores/metrics/MetricsStore';

import { Spinner } from 'components/common';
import { MetricsFilterInput, MetricsList } from 'components/metrics';

const MetricsComponent = React.createClass({
  mixins: [LinkedStateMixin, Reflux.connect(MetricsStore)],
  getInitialState() {
    return { filter: '' };
  },
  render() {
    if (!this.state.names) {
      return <Spinner />;
    }

    const filteredNames = this.state.names
      .filter((metric) => String(metric.full_name).contains(this.state.filter));
    return (
      <Row className="content">
        <Col md={12}>
          <MetricsFilterInput valueLink={this.linkState('filter')} />
          <MetricsList names={filteredNames} namespace={MetricsStore.namespace}/>
        </Col>
      </Row>
    );
  },
});

export default MetricsComponent;
