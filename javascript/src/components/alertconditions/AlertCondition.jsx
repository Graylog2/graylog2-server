import React from 'react';
import { Row, Col, Badge, Button } from 'react-bootstrap';

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

import AlertConditionsActions from 'actions/alertconditions/AlertConditionsActions';

import UnknownAlertCondition from 'components/alertconditions/UnknownAlertCondition';
import AlertConditionForm from 'components/alertconditions/AlertConditionForm';

const AlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      edit: false,
    };
  },
  _onEdit() {
    this.setState({edit: !this.state.edit});
  },
  _onDelete() {
    if (window.confirm('Really delete alarm condition?')) {
      AlertConditionsActions.delete(this.props.alertCondition.stream_id, this.props.alertCondition.id);
    }
  },
  _onUpdate() {
    const request = {
      type: this.props.alertCondition.type,
      parameters: this.refs.updateForm.getValue(),
    }
    AlertConditionsActions.update.triggerPromise(this.props.alertCondition.stream_id, this.props.alertCondition.id, request)
    .then(() => {
      this.setState({edit: false});
    });
  },
  render() {
    const alertCondition = this.props.alertCondition;
    const alertConditionType = new AlertConditionsFactory().get(alertCondition.type);
    if (!alertConditionType) {
      return <UnknownAlertCondition alertCondition={alertCondition} />;
    }
    return (
      <span>
        <Row className="alert-condition" data-condition-id={alertCondition.id}>
          <Col md={9}>
            <h3>{alertConditionType.title} condition {alertCondition.in_grace && <Badge className="badge-info">in grace period</Badge>}</h3>
            <alertConditionType.summary alertCondition={alertCondition} />
            {' '}
            {this.state.edit &&
            <span>
              <AlertConditionForm ref="updateForm" type={alertCondition.type} alertCondition={alertCondition.parameters} />
              {' '}
              <Button bsStyle="info" onClick={this._onUpdate}>Save</Button>
            </span>}
          </Col>

          <Col md={3} style={{textAlign: 'right'}}>
            <Button bsStyle="success" onClick={this._onEdit}>Edit condition</Button>
            {' '}
            <Button bsStyle="danger" onClick={this._onDelete}>Delete condition</Button>
          </Col>
        </Row>
        <hr />
      </span>
    );
  },
});

export default AlertCondition;
