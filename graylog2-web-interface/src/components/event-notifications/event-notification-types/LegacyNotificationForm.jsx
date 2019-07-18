import React from 'react';
import PropTypes from 'prop-types';
import { Alert, ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';
import lodash from 'lodash';

import { Select } from 'components/common';
import { ConfigurationFormField } from 'components/configurationforms';

class LegacyNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  propagateMultiChange = (newValues) => {
    const { config, onChange } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    Object.entries(newValues).forEach((keyAndValue) => {
      const [key, value] = keyAndValue;
      nextConfig[key] = value;
    });

    onChange(nextConfig);
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfiguration = lodash.cloneDeep(config.configuration);
    nextConfiguration[key] = value;
    this.propagateMultiChange({ configuration: nextConfiguration });
  };

  formatLegacyTypes = (legacyTypes) => {
    return Object.keys(legacyTypes)
      .map(typeName => ({ label: `Legacy ${legacyTypes[typeName].name}`, value: typeName }));
  };

  handleSelectNotificationChange = (nextLegacyNotificationType) => {
    this.propagateMultiChange({
      callback_type: nextLegacyNotificationType,
      configuration: {},
    });
  };

  handleFormFieldChange = (key, value) => {
    this.propagateChange(key, value);
  };

  renderNotificationForm(config, legacyType) {
    const { configuration } = legacyType;

    const configFields = Object.keys(configuration).map((configKey) => {
      const configField = configuration[configKey];
      const configValue = config.configuration[configKey] || configField.default_value;

      return (
        <ConfigurationFormField typeName={config.callback_type}
                                configField={configField}
                                configKey={configKey}
                                configValue={configValue}
                                onChange={this.handleFormFieldChange} />
      );
    });

    return (
      <fieldset>
        {configFields}
      </fieldset>
    );
  }

  render() {
    const { config, legacyTypes } = this.props;
    const callbackType = config.callback_type;

    return (
      <React.Fragment>
        <fieldset>
          <FormGroup controlId="notification-legacy-select">
            <ControlLabel>Choose Legacy Notification</ControlLabel>
            <Select id="notification-legacy-select"
                    matchProp="label"
                    placeholder="Select Legacy Notification"
                    onChange={this.handleSelectNotificationChange}
                    options={this.formatLegacyTypes(legacyTypes)}
                    value={callbackType} />
            <HelpBlock>Select a Legacy Notification to use on this Event Definition.</HelpBlock>
          </FormGroup>
        </fieldset>

        <Alert bsStyle="danger">
          Legacy alarm callbacks are deprecated. Please switch to the new notification types as soon as possible!
        </Alert>

        {callbackType && this.renderNotificationForm(config, legacyTypes[callbackType])}
      </React.Fragment>
    );
  }
}

export default LegacyNotificationForm;
