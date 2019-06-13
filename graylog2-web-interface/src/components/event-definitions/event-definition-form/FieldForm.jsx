import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, Col, ControlLabel, FormControl, FormGroup, InputGroup, Row } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

import FormsUtils from 'util/FormsUtils';

// TODO: Make this pluggable
import TemplateFieldValueProviderForm from './field-value-providers/TemplateFieldValueProviderForm';

class FieldForm extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string,
    config: PropTypes.object,
    keys: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    onRemoveField: PropTypes.func.isRequired,
  };

  static defaultProps = {
    fieldName: '',
    config: {},
    keys: [],
  };

  handleRemoveField = () => {
    const { fieldName, onRemoveField } = this.props;
    onRemoveField(fieldName);
  };

  handleFieldNameChange = (event) => {
    const { fieldName, onChange } = this.props;
    const nextValue = FormsUtils.getValueFromInput(event.target);
    onChange(fieldName, 'fieldName', nextValue);
  };

  propagateConfigChange = (nextConfig) => {
    const { fieldName, onChange } = this.props;
    onChange(fieldName, 'config', nextConfig);
  };

  handleConfigChange = (event) => {
    const { config } = this.props;
    const key = event.target.name;
    const value = FormsUtils.getValueFromInput(event.target);
    const nextConfig = lodash.cloneDeep(config);
    nextConfig[key] = value;
    this.propagateConfigChange(nextConfig);
  };

  handleProviderTypeChange = (nextProvider) => {
    const { config } = this.props;
    const nextConfig = Object.assign({}, config, { providers: [{ type: nextProvider }] });
    this.propagateConfigChange(nextConfig);
  };

  renderFieldValueProviderForm = () => {
    const { fieldName, config } = this.props;
    if (!config.providers || !Array.isArray(config.providers)) {
      return null;
    }

    switch (config.providers[0].type) {
      case 'template-v1':
        return <TemplateFieldValueProviderForm fieldName={fieldName} config={config} onChange={this.propagateConfigChange} />;
      case 'lookup-v1':
        return <div>TBD</div>;
      default:
        return <div>Selected provider is not available</div>;
    }
  };

  toggleKey = (event) => {
    const { fieldName, keys, onChange } = this.props;
    const checked = FormsUtils.getValueFromInput(event.target);

    let nextKeys;
    if (checked) {
      nextKeys = lodash.cloneDeep(keys);
      nextKeys.push(fieldName);
    } else {
      nextKeys = lodash.filter(keys, fieldName);
    }

    onChange(fieldName, 'keys', nextKeys);
  };

  render() {
    const { fieldName, config, keys } = this.props;
    const isKeyEnabled = keys.includes(fieldName);
    // Get the sorted position this field in the keys or the next available key
    const keyValue = (keys.indexOf(fieldName) < 0 ? keys.length : keys.indexOf(fieldName)) + 1;

    return (
      <form onSubmit={this.handleSubmit}>
        <fieldset>
          <legend>
            Custom Field
            <span className="pull-right">
              <Button bsSize="xsmall" bsStyle="primary" onClick={this.handleRemoveField}>Remove</Button>
            </span>
          </legend>

          <Row className="row-sm">
            <Col md={4}>
              <Input id="field-name"
                     name="name"
                     label="Name"
                     type="text"
                     value={fieldName}
                     onChange={this.handleFieldNameChange}
                     required />
            </Col>
            <Col md={4}>
              <FormGroup>
                <ControlLabel>Use as key <span className="fa fa-question-circle" /></ControlLabel>
                <InputGroup>
                  <InputGroup.Addon>
                    <input type="checkbox" onChange={this.toggleKey} checked={isKeyEnabled} />
                  </InputGroup.Addon>
                  <FormControl id="field-key"
                               name="key"
                               type="number"
                               value={keyValue}
                               onChange={this.handleConfigChange}
                               disabled={!isKeyEnabled} />
                </InputGroup>
              </FormGroup>
            </Col>
            <Col md={4}>
              <FormGroup controlId="event-field-provider">
                <ControlLabel>Set Value From</ControlLabel>
                <Select name="event-field-provider"
                        placeholder="Select Value Source"
                        onChange={this.handleProviderTypeChange}
                        options={[
                          { value: 'template-v1', label: 'Template' },
                          { value: 'lookup-v1', label: 'Lookup Table' },
                        ]}
                        value={Array.isArray(config.providers) ? config.providers[0].type : ''}
                        matchProp="label"
                        required />
              </FormGroup>
            </Col>
          </Row>
          {this.renderFieldValueProviderForm()}
        </fieldset>
      </form>
    );
  }
}

export default FieldForm;
