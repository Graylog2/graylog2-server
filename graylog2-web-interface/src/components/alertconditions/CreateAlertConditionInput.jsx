import React from 'react';
import Reflux from 'reflux';

import { Row, Col, Input, Button } from 'react-bootstrap';
import jQuery from 'jquery';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsStore, AlertConditionsActions } = CombinedProvider.get('AlertConditions');

import AlertConditionForm from 'components/alertconditions/AlertConditionForm';

const CreateAlertConditionInput = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],
  getInitialState() {
    return {
      type: this.PLACEHOLDER,
    };
  },
  PLACEHOLDER: 'placeholder',
  _onChange(evt) {
    this.setState({ type: evt.target.value });
  },
  _onSubmit(request) {
    AlertConditionsActions.save(this.props.streamId, request);
    this._resetForm();
  },
  _openForm() {
    this.refs.configurationForm.open();
  },
  _resetForm() {
    this.setState(this.getInitialState());
  },
  _formatConditionForm(type) {
    return (<AlertConditionForm ref="configurationForm"
                                onSubmit={this._onSubmit}
                                type={type} />);

  },
  render() {
    const conditionForm = (this.state.type !== this.PLACEHOLDER ? this._formatConditionForm(this.state.type) : null);
    const availableTypes = jQuery.map(this.state.types, (definition, value) => {
      return <option key={`type-option-${value}`} value={value}>{definition.human_name}</option>;
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

          <form className="form-inline">
            <div className="form-group" style={{ display: 'block' }}>
              <Input type="select" className="add-alert-type form-control" value={this.state.type} onChange={this._onChange}>
                <option value={this.PLACEHOLDER} disabled>Select Alert Condition Type</option>
                {availableTypes}
              </Input>
              {conditionForm}
              {' '}
              <Button onClick={this._openForm} disabled={this.state.type === this.PLACEHOLDER || this.props.disabled}
                      bsStyle="success" className="form-control add-alert">
                Create new alert condition
              </Button>
            </div>
          </form>
        </Col>
      </Row>
    );
  },
});

export default CreateAlertConditionInput;
