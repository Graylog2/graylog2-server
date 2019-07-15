import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, Col, Row } from 'react-bootstrap';

import FieldsList from './FieldsList';

// Import built-in Field Value Providers
import {} from './field-value-providers';

import commonStyles from '../common/commonStyles.css';

class FieldsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    showAddFieldForm: false,
  };

  addCustomField = () => {
    const { eventDefinition, onChange } = this.props;
    const nextFieldSpec = Object.assign({}, eventDefinition.field_spec, { '': {} });
    onChange('field_spec', nextFieldSpec);
  };

  removeCustomField = (fieldName) => {
    const { eventDefinition, onChange } = this.props;
    const nextFieldSpec = lodash.omit(eventDefinition.field_spec, fieldName);
    onChange('field_spec', nextFieldSpec);
  };

  handleFieldChange = (fieldName, key, value) => {
    const { eventDefinition, onChange } = this.props;
    if (key === 'keys') {
      onChange('key_spec', value);
    } else {
      let nextFieldSpec;

      if (key === 'config') {
        nextFieldSpec = lodash.cloneDeep(eventDefinition.field_spec);
        nextFieldSpec[fieldName] = value;
      } else if (key === 'fieldName') {
        const config = eventDefinition.field_spec[fieldName];
        nextFieldSpec = lodash.omit(eventDefinition.field_spec, fieldName);
        let effectiveValue = value;
        while (Object.keys(eventDefinition.field_spec).includes(effectiveValue)) {
          effectiveValue += '_';
        }
        nextFieldSpec[effectiveValue] = config;
      }

      onChange('field_spec', nextFieldSpec);
    }
  };

  toggleAddFieldForm = () => {
    const { showAddFieldForm } = this.state;
    this.setState({ showAddFieldForm: !showAddFieldForm });
  };

  render() {
    const { eventDefinition } = this.props;
    const { showAddFieldForm } = this.state;


    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Event Fields</h2>
          <FieldsList fields={eventDefinition.field_spec}
                      keys={eventDefinition.key_spec}
                      onAddFieldClick={this.toggleAddFieldForm}
                      onRemoveFieldClick={this.removeCustomField} />
        </Col>
      </Row>
    );
  }
}

export default FieldsForm;
