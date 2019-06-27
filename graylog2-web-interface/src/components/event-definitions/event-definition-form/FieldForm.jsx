import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, Col, ControlLabel, FormControl, FormGroup, InputGroup, Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

import FormsUtils from 'util/FormsUtils';

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

  getProviderPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('fieldValueProviders').find(edt => edt.type === type);
  };

  renderFieldValueProviderForm = () => {
    const { fieldName, config } = this.props;
    if (!config.providers || !Array.isArray(config.providers)) {
      return null;
    }

    const providerPlugin = this.getProviderPlugin(config.providers[0].type);
    return providerPlugin.formComponent
      ? React.createElement(providerPlugin.formComponent, {
        fieldName: fieldName,
        config: config,
        onChange: this.propagateConfigChange,
      })
      : <div>Selected provider is not available.</div>;
  };

  handleKeySortChange = (event) => {
    const { fieldName, keys, onChange } = this.props;
    const nextPosition = FormsUtils.getValueFromInput(event.target) - 1;

    if (nextPosition < 0 || nextPosition >= keys.length) {
      // TODO: Display an error when this happens
      return;
    }

    // Remove fieldName from previous position and add it to the given one
    let nextKeys = lodash.without(keys, fieldName);
    if (nextPosition === 0) {
      nextKeys.unshift(fieldName);
    } else if (nextPosition >= nextKeys.length) {
      nextKeys.push(fieldName);
    } else {
      nextKeys = [...nextKeys.slice(0, nextPosition), fieldName, ...nextKeys.slice(nextPosition)];
    }

    onChange(fieldName, 'keys', nextKeys);
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

  formatFieldValueProviders = () => {
    return PluginStore.exports('fieldValueProviders')
      .map(type => ({ label: type.displayName, value: type.type }));
  };

  render() {
    const { fieldName, config, keys } = this.props;
    const isKeyEnabled = keys.includes(fieldName);
    // Get the sorted position this field in the keys or the next available key
    const keyValue = (keys.indexOf(fieldName) < 0 ? keys.length : keys.indexOf(fieldName)) + 1;

    return (
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
                             onChange={this.handleKeySortChange}
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
                      options={this.formatFieldValueProviders()}
                      value={Array.isArray(config.providers) ? config.providers[0].type : ''}
                      matchProp="label"
                      required />
            </FormGroup>
          </Col>
        </Row>
        {this.renderFieldValueProviderForm()}
      </fieldset>
    );
  }
}

export default FieldForm;
