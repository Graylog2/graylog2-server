import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { MetricsList } from 'components/metrics';

const MetricsComponent = React.createClass({
  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <MetricsList />
        </Col>
      </Row>
    );
  },
});

export default MetricsComponent;
