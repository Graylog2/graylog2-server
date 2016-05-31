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
  getInitialState() {
    return {
      edit: false,
    };
  },
  _onEdit() {
    this.setState({ edit: !this.state.edit });
  },
  _onDelete() {
    if (window.confirm('Really delete alarm condition?')) {
      AlertConditionsActions.delete(this.props.alertCondition.stream_id, this.props.alertCondition.id);
    }
  },
  _onUpdate(event) {
    event.preventDefault();
    const request = this.refs.updateForm.getValue();
    request.type = this.props.alertCondition.type;
    AlertConditionsActions.update.triggerPromise(this.props.alertCondition.stream_id, this.props.alertCondition.id, request)
    .then(() => {
      this.setState({ edit: false });
    });
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
            <h3>{this._formatTitle()}</h3>
            <alertConditionType.summary alertCondition={alertCondition} />
            {' '}
            {this.state.edit &&
            <form onSubmit={this._onUpdate}>
              <AlertConditionForm ref="updateForm" type={alertCondition.type} alertCondition={alertCondition} />
              {' '}
              <Button bsStyle="info" type="submit">Save</Button>
            </form>}
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
