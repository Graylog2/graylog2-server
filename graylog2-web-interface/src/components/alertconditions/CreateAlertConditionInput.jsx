import React from 'react';
import { Row, Col, Input, Button } from 'react-bootstrap';
import jQuery from 'jquery';

import AlertConditionsActions from 'actions/alertconditions/AlertConditionsActions';

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
    this.setState({type: evt.target.value});
  },
  _onSubmit(evt) {
    evt.preventDefault();
    const request = {
      type: this.state.type,
      parameters: this.refs.conditionForm.getValue(),
    };
    AlertConditionsActions.save(this.props.streamId, request);
  },
  _formatConditionForm(type) {
    return <AlertConditionForm ref="conditionForm" type={type}/>;
  },
  render() {
    const conditionForm = (this.state.type !== this.PLACEHOLDER ? this._formatConditionForm(this.state.type) : null);
    const availableTypes = jQuery.map(this.alertConditionsFactory.available(), (definition, value) => {
      return <option key={'type-option-' + value} value={value}>{definition.title} condition</option>;
    });
    return (
      <Row className="content input-new">
        <Col md={12}>
          <h2 style={{marginBotton: '10px'}}>
            Add new alert condition
          </h2>
          <p className="description">
            Configure conditions that will trigger stream alerts when they are fulfilled.
          </p>

          <form className="form-inline" onSubmit={this.props.onSubmit}>
            <div className="form-group" style={{display: 'block'}}>
              <Input type="select" className="add-alert-type form-control" value={this.state.type} onChange={this._onChange}>
                <option value={this.PLACEHOLDER} disabled>Select Alert Condition Type</option>
                {availableTypes}
              </Input>
              {' '}
              <Button type="submit" bsStyle="success" className="form-control add-alert" disabled={this.state.type === this.PLACEHOLDER}>
                Create new alert condition
              </Button>

              {conditionForm}
            </div>
          </form>
        </Col>
      </Row>
    );
  },
});

export default CreateAlertConditionInput;
