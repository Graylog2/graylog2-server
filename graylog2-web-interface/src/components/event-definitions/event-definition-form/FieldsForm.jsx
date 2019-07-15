import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, Row } from 'react-bootstrap';

import FieldForm from './FieldForm';
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

  removeCustomField = (fieldName) => {
    const { eventDefinition, onChange } = this.props;
    const nextFieldSpec = lodash.omit(eventDefinition.field_spec, fieldName);
    onChange('field_spec', nextFieldSpec);
    const nextKeySpec = lodash.without(eventDefinition.key_spec, fieldName);
    onChange('key_spec', nextKeySpec);
  };

  addCustomField = (prevFieldName, fieldName, config, isKey, keyPosition) => {
    const { eventDefinition, onChange } = this.props;
    const nextFieldSpec = (prevFieldName === fieldName
      ? lodash.cloneDeep(eventDefinition.field_spec)
      : lodash.omit(eventDefinition.field_spec, prevFieldName));
    nextFieldSpec[fieldName] = config;
    onChange('field_spec', nextFieldSpec);

    // Filter out all non-existing field names from key_spec and the current field name
    const fieldNames = Object.keys(nextFieldSpec);
    let nextKeySpec = eventDefinition.key_spec.filter(key => fieldNames.includes(key) && key !== fieldName);
    if (isKey) {
      // Add key to its new position
      nextKeySpec = [...nextKeySpec.slice(0, keyPosition), fieldName, ...nextKeySpec.slice(keyPosition)];
    }
    onChange('key_spec', nextKeySpec);

    this.toggleAddFieldForm();
  };

  toggleAddFieldForm = () => {
    const { showAddFieldForm } = this.state;
    this.setState({ showAddFieldForm: !showAddFieldForm });
  };

  render() {
    const { eventDefinition } = this.props;
    const { showAddFieldForm } = this.state;

    if (showAddFieldForm) {
      return (
        <FieldForm keys={eventDefinition.key_spec}
                   onChange={this.addCustomField}
                   onCancel={this.toggleAddFieldForm} />
      );
    }

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
