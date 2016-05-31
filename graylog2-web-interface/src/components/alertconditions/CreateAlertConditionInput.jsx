import React from 'react';
import { Row, Col, Input, Button } from 'react-bootstrap';
import jQuery from 'jquery';

import ActionsProvider from 'injection/ActionsProvider';
const AlertConditionsActions = ActionsProvider.getActions('AlertConditions');

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

import AlertConditionForm from 'components/alertconditions/AlertConditionForm';

const CreateAlertConditionInput = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string.isRequired,
  },
  getInitialState() {
    return {
      type: this.PLACEHOLDER,
    };
  },
  PLACEHOLDER: 'placeholder',
  alertConditionsFactory: new AlertConditionsFactory(),
  _onChange(evt) {
    this.setState({ type: evt.target.value });
  },
  _resetForm() {
    this.setState(this.getInitialState());
  },
  _onSubmit(evt) {
    evt.preventDefault();
    const request = this.refs.conditionForm.getValue();
    request.type = this.state.type;
    AlertConditionsActions.save.triggerPromise(this.props.streamId, request).then(() => { this._resetForm(); });
  },
  _formatConditionForm(type) {
    return <AlertConditionForm ref="conditionForm" type={type}/>;
  },
  render() {
    const conditionForm = (this.state.type !== this.PLACEHOLDER ? this._formatConditionForm(this.state.type) : null);
    const availableTypes = jQuery.map(this.alertConditionsFactory.available(), (definition, value) => {
      return <option key={`type-option-${value}`} value={value}>{definition.title} condition</option>;
    });
    return (
      <Row className="content input-new">
        <Col md={12}>
          <h2 style={{ marginBotton: '10px' }}>
            Add new alert condition
          </h2>
          <p className="description">
            Configure conditions that will trigger stream alerts when they are fulfilled.
          </p>

          <form className="form-inline" onSubmit={this._onSubmit}>
            <div className="form-group" style={{ display: 'block' }}>
              <Input type="select" className="add-alert-type form-control" value={this.state.type} onChange={this._onChange}>
                <option value={this.PLACEHOLDER} disabled>Select Alert Condition Type</option>
                {availableTypes}
              </Input>
              {conditionForm}
              {conditionForm !== null && <Button type="submit" bsStyle="success" className="form-control add-alert">
                Create new alert condition
              </Button>}
            </div>
          </form>
        </Col>
      </Row>
    );
  },
});

export default CreateAlertConditionInput;
