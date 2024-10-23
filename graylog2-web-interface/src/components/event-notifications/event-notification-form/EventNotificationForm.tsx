/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import get from 'lodash/get';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormSubmit, Select, Spinner } from 'components/common';
import { Alert, Button, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

const getNotificationPlugin = (type: string) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type);
};

const formattedEventNotificationTypes = () => PluginStore.exports('eventNotificationTypes')
  .map((type) => ({ label: type.displayName, value: type.type }));

type EventNotificationFormProps = {
  action?: 'create' | 'edit';
  notification: any;
  validation: any;
  testResult: {
    isLoading?: boolean;
    error?: boolean;
    message?: string;
  };
  formId?: string;
  embedded: boolean;
  onChange: (...args: any[]) => void;
  onCancel: (...args: any[]) => void;
  onSubmit: (...args: any[]) => void;
  onTest: (...args: any[]) => void;
  sendTelemetry: (...args: any[]) => void;
  location: any;
};

class EventNotificationForm extends React.Component<EventNotificationFormProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    action: 'edit',
    formId: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      isSubmitEnabled: true,
    };
  }

  setIsSubmitEnabled = (enabled) => {
    this.setState({ isSubmitEnabled: enabled });
  };

  handleSubmit = (event) => {
    const { notification, onSubmit, sendTelemetry, action, location } = this.props;

    sendTelemetry(
      action === 'create'
        ? TELEMETRY_EVENT_TYPE.NOTIFICATIONS.CREATE_CLICKED
        : TELEMETRY_EVENT_TYPE.NOTIFICATIONS.EDIT_CLICKED, {
        app_pathname: getPathnameWithoutId(location.pathname),
        app_section: 'event-notification',
        app_action_value: `${action}-button`,
      });

    event.preventDefault();

    onSubmit(notification);
  };

  handleChange = (event) => {
    const { name } = event.target;
    const { onChange } = this.props;

    onChange(name, getValueFromInput(event.target));
  };

  handleConfigChange = (nextConfig) => {
    const { onChange } = this.props;

    onChange('config', nextConfig);
  };

  handleTypeChange = (nextType) => {
    const { sendTelemetry, location } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.NOTIFICATION_TYPE_SELECTED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'event-definition-notifications',
      app_action_value: 'notification-type-select',
      notification_type: nextType,
    });

    const notificationPlugin = getNotificationPlugin(nextType);
    const defaultConfig = notificationPlugin?.defaultConfig || {};

    this.handleConfigChange({ ...defaultConfig, type: nextType });
  };

  handleTestTrigger = () => {
    const { notification, onTest, sendTelemetry, location } = this.props;

    sendTelemetry(TELEMETRY_EVENT_TYPE.NOTIFICATIONS.EXECUTE_TEST_CLICKED, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'event-notification',
      app_action_value: 'execute-test-button',
    });

    onTest(notification);
  };

  render() {
    const { action, embedded, formId, notification, onCancel, validation, testResult } = this.props;
    const { isSubmitEnabled } = this.state;

    const notificationPlugin = getNotificationPlugin(notification.config.type);
    const notificationFormComponent = notificationPlugin?.formComponent
      ? React.createElement(notificationPlugin.formComponent, {
        config: notification.config,
        onChange: this.handleConfigChange,
        validation: validation,
        setIsSubmitEnabled: this.setIsSubmitEnabled,
      })
      : null;

    const testButtonText = testResult.isLoading ? <Spinner text="Testing..." /> : 'Execute Test Notification';

    return (
      <Row>
        <Col lg={8}>
          <form onSubmit={this.handleSubmit} id={formId}>
            <Input id="notification-title"
                   name="title"
                   label="Title"
                   type="text"
                   bsStyle={validation.errors.title ? 'error' : null}
                   help={get(validation, 'errors.title[0]', 'Title to identify this Notification.')}
                   value={notification.title}
                   onChange={this.handleChange}
                   required
                   autoFocus />

            <Input id="notification-description"
                   name="description"
                   label={<span>Description <small className="text-muted">(Optional)</small></span>}
                   type="textarea"
                   help="Longer description for this Notification."
                   value={notification.description}
                   onChange={this.handleChange}
                   rows={2} />

            <FormGroup controlId="notification-type" validationState={validation.errors.config ? 'error' : null}>
              <ControlLabel>Notification Type</ControlLabel>
              <Select id="notification-type"
                      options={formattedEventNotificationTypes()}
                      value={notification.config.type}
                      onChange={this.handleTypeChange}
                      clearable={false}
                      required />
              <HelpBlock>
                {get(validation, 'errors.config[0]', 'Choose the type of Notification to create.')}
              </HelpBlock>
            </FormGroup>

            {notificationFormComponent}

            {notificationFormComponent && (
              <FormGroup>
                <ControlLabel>Test Notification <small className="text-muted">(Optional)</small></ControlLabel>
                <FormControl.Static>
                  <Button bsStyle="info"
                          bsSize="small"
                          disabled={testResult.isLoading}
                          onClick={this.handleTestTrigger}>
                    {testButtonText}
                  </Button>
                </FormControl.Static>
                {testResult.message && (
                  <Alert bsStyle={testResult.error ? 'danger' : 'success'} title={testResult.error ? 'Error: ' : 'Success: '}>
                    {testResult.message}
                  </Alert>
                )}
                <HelpBlock>
                  Execute this Notification with a test Alert.
                </HelpBlock>
              </FormGroup>
            )}

            {!embedded && (
              <FormSubmit disabledSubmit={!isSubmitEnabled}
                          submitButtonText={`${action === 'create' ? 'Create' : 'Update'} notification`}
                          onCancel={onCancel} />
            )}
          </form>
        </Col>
      </Row>
    );
  }
}

export default withLocation(withTelemetry(EventNotificationForm));
