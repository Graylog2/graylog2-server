import React from 'react';
import { Row, Col, Badge } from 'react-bootstrap';

const UnknownAlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <span>
        <Row className="alert-condition" data-condition-id={alertCondition.id}>
          <Col md={9}>
            <h3>Unknown alert condition ({alertCondition.type}) {alertCondition.in_grace && <Badge className="badge-info">in grace period</Badge>}</h3>
          </Col>

          <Col md={3} style={{textAlign: 'right'}}>
          </Col>
        </Row>
        <hr />
      </span>
    );
  },
});

export default UnknownAlertCondition;
