import React from 'react';
import { Row, Col, Badge, Button } from 'react-bootstrap';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsActions } = CombinedProvider.get('AlertConditions');

const UnknownAlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _onDelete() {
    if (window.confirm('Really delete alarm condition?')) {
      AlertConditionsActions.delete(this.props.alertCondition.stream_id, this.props.alertCondition.id);
    }
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
            <Button bsStyle="danger" onClick={this._onDelete}>Delete condition</Button>
          </Col>
        </Row>
        <hr />
      </span>
    );
  },
});

export default UnknownAlertCondition;
