import React from 'react';
import PropTypes from 'prop-types';
import {
  Button,
  ButtonToolbar,
  Col,
  ControlLabel,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  OverlayTrigger,
  Row,
} from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';
import EventKeyHelpPopover from 'components/event-definitions/common/EventKeyHelpPopover';

import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

class FieldForm extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string,
    config: PropTypes.object,
    keys: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
  };

  static defaultProps = {
    fieldName: '',
    config: {},
  };

  constructor(props) {
    super(props);
    const keyIndex = props.keys.indexOf(props.fieldName);

    this.state = {
      fieldName: props.fieldName,
      config: Object.assign({}, props.config, { data_type: 'string' }),
      isKey: keyIndex >= 0,
      keyPosition: keyIndex >= 0 ? keyIndex + 1 : props.keys.length + 1,
    };
  }

  handleSubmit = () => {
    const { fieldName: prevFieldName, onChange } = this.props;
    const { fieldName, config, isKey, keyPosition } = this.state;
    onChange(prevFieldName, fieldName, config, isKey, keyPosition - 1);
  };

  handleFieldNameChange = (event) => {
    const nextFieldName = FormsUtils.getValueFromInput(event.target);
    this.setState({ fieldName: nextFieldName });
  };

  handleConfigChange = (nextConfig) => {
    this.setState({ config: nextConfig });
  };

  handleProviderTypeChange = (nextProvider) => {
    const { config } = this.state;
    const nextConfig = Object.assign({}, config, { providers: [{ type: nextProvider }] });
    this.handleConfigChange(nextConfig);
  };

  handleKeySortChange = (event) => {
    const nextPosition = FormsUtils.getValueFromInput(event.target);
    this.setState({ keyPosition: nextPosition });
  };

  toggleKey = (event) => {
    const checked = FormsUtils.getValueFromInput(event.target);
    this.setState({ isKey: checked });
  };

  getProviderPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('fieldValueProviders').find(edt => edt.type === type);
  };

  renderFieldValueProviderForm = () => {
    const { fieldName, config } = this.state;
    if (!config.providers || !Array.isArray(config.providers)) {
      return null;
    }

    const providerPlugin = this.getProviderPlugin(config.providers[0].type);
    return providerPlugin.formComponent
      ? React.createElement(providerPlugin.formComponent, {
        fieldName: fieldName,
        config: config,
        onChange: this.handleConfigChange,
      })
      : <div>Selected provider is not available.</div>;
  };

  formatFieldValueProviders = () => {
    return PluginStore.exports('fieldValueProviders')
      .map(type => ({ label: type.displayName, value: type.type }));
  };

  render() {
    const { fieldName: prevFieldName, onCancel } = this.props;
    const { fieldName, isKey, keyPosition, config } = this.state;

    return (
      <Row>
        <Col md={8} lg={6}>
          <h2 className={commonStyles.title}>
            {prevFieldName ? `Custom Field "${fieldName}"` : 'New Custom Field'}
          </h2>

          <Input id="field-name"
                 name="name"
                 label="Name"
                 type="text"
                 value={fieldName}
                 onChange={this.handleFieldNameChange}
                 help="Name for this Field."
                 required />

          <FormGroup>
            <ControlLabel>
              Use Field as Event Key&emsp;
              <OverlayTrigger placement="right" trigger="click" overlay={<EventKeyHelpPopover id="key-popover" />}>
                <Button bsStyle="link" bsSize="xsmall"><i className="fa fa-question-circle" /></Button>
              </OverlayTrigger>
            </ControlLabel>
            <InputGroup>
              <InputGroup.Addon>
                <input id="is-key" name="is-key" type="checkbox" onChange={this.toggleKey} checked={isKey} />
              </InputGroup.Addon>
              <FormControl id="field-key"
                           name="key"
                           type="number"
                           value={keyPosition}
                           onChange={this.handleKeySortChange}
                           disabled={!isKey} />
            </InputGroup>
            <HelpBlock>Indicates if this Field should be a Key and its order.</HelpBlock>
          </FormGroup>

          <FormGroup>
            <ControlLabel>Field Data Type</ControlLabel>
            <FormControl.Static>String</FormControl.Static>
          </FormGroup>

          <FormGroup controlId="event-field-provider">
            <ControlLabel>Set Value From</ControlLabel>
            <Select name="event-field-provider"
                    placeholder="Select Value Source"
                    onChange={this.handleProviderTypeChange}
                    options={this.formatFieldValueProviders()}
                    value={Array.isArray(config.providers) ? config.providers[0].type : ''}
                    matchProp="label"
                    required />
            <HelpBlock>Select a source for the value of this Field.</HelpBlock>
          </FormGroup>

          {this.renderFieldValueProviderForm()}

          <ButtonToolbar>
            <Button bsStyle="primary" onClick={this.handleSubmit}>Done</Button>
            <Button onClick={onCancel}>Cancel</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default FieldForm;
