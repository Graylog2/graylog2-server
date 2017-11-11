import PropTypes from 'prop-types';
import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import { Col, Row } from 'react-bootstrap';

import { MetricsFilterInput, MetricsList } from 'components/metrics';

const MetricsComponent = React.createClass({
  propTypes: {
    names: PropTypes.arrayOf(PropTypes.object).isRequired,
    namespace: PropTypes.string.isRequired,
    nodeId: PropTypes.string.isRequired,
  },
  mixins: [LinkedStateMixin],
  getInitialState() {
    return { filter: this.props.filter };
  },
  getDefaultProps() {
    return { filter: '' };
  },
  render() {
    let filteredNames;
    try {
      const filter = new RegExp(this.state.filter, 'i');
      filteredNames = this.props.names
        .filter(metric => String(metric.full_name).match(filter));
    } catch (e) {
      filteredNames = [];
    }
    return (
      <Row className="content">
        <Col md={12}>
          <MetricsFilterInput valueLink={this.linkState('filter')} />
          <MetricsList names={filteredNames} namespace={this.props.namespace} nodeId={this.props.nodeId} />
        </Col>
      </Row>
    );
  },
});

export default MetricsComponent;
