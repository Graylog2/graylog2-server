import React from 'react';
import { Row, Col, Badge, Button } from 'react-bootstrap';

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

import ActionsProvider from 'injection/ActionsProvider';
const AlertConditionsActions = ActionsProvider.getActions('AlertConditions');

import UnknownAlertCondition from 'components/alertconditions/UnknownAlertCondition';
import AlertConditionForm from 'components/alertconditions/AlertConditionForm';

const AlertCondition = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
  },
  _onEdit() {
    this.refs.updateForm.open();
  },
  _onDelete() {
    if (window.confirm('Really delete alarm condition?')) {
      AlertConditionsActions.delete(this.props.alertCondition.stream_id, this.props.alertCondition.id);
    }
  },
  _onUpdate(request) {
    AlertConditionsActions.update.triggerPromise(this.props.alertCondition.stream_id, this.props.alertCondition.id, request);
  },
  _formatTitle() {
    const alertCondition = this.props.alertCondition;
    const alertConditionType = new AlertConditionsFactory().get(alertCondition.type);
    const title = alertCondition.title ? alertCondition.title : 'Untitled';
    const subtitle = `(${alertConditionType.title} condition)`;
    const badge = alertCondition.in_grace && <Badge className="badge-info">in grace period</Badge>;
    return (
      <span>
        {title} <small>{subtitle}</small> {badge}
      </span>
    );
  },
  alertConditionsFactory: new AlertConditionsFactory(),
  render() {
    const alertCondition = this.props.alertCondition;
    const alertConditionType = this.alertConditionsFactory.get(alertCondition.type);
    if (!alertConditionType || alertConditionType.length === 0) {
      return <UnknownAlertCondition alertCondition={alertCondition} />;
    }
    const alertConditionSummary = alertConditionType[0].summary;
    return (
      <span>
        <Row className="alert-condition" data-condition-id={alertCondition.id}>
          <Col md={9}>
            <h3>{this._formatTitle()}</h3>
            <alertConditionSummary alertCondition={alertCondition} />
            <AlertConditionForm ref="updateForm"
                                type={alertCondition.type}
                                alertCondition={alertCondition}
                                onSubmit={this._onUpdate}/>
          </Col>

          <Col md={3} style={{ textAlign: 'right' }}>
            <Button bsStyle="success" onClick={this._onEdit}>Edit condition</Button>
            {' '}
            <Button bsStyle="danger" onClick={this._onDelete}>Delete condition</Button>
          </Col>
        </Row>
      </span>
    );
  },
});

export default AlertCondition;
