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
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Alert, Button, ButtonToolbar, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Select, Spinner } from 'components/common';
import { Input } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

class EventNotificationForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    notification: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    testResult: PropTypes.shape({
      isLoading: PropTypes.bool,
      error: PropTypes.bool,
      message: PropTypes.string,
    }).isRequired,
    formId: PropTypes.string,
    embedded: PropTypes.bool.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onTest: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    formId: undefined,
  };

  handleSubmit = (event) => {
    const { notification, onSubmit } = this.props;

    event.preventDefault();

    onSubmit(notification);
  };

  handleChange = (event) => {
    const { name } = event.target;
    const { onChange } = this.props;

    onChange(name, FormsUtils.getValueFromInput(event.target));
  };

  handleConfigChange = (nextConfig) => {
    const { onChange } = this.props;

    onChange('config', nextConfig);
  };

  getNotificationPlugin = (type) => {
    if (type === undefined) {
      return {};
    }

    return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type) || {};
  };

  handleTypeChange = (nextType) => {
    const notificationPlugin = this.getNotificationPlugin(nextType);
    const defaultConfig = notificationPlugin.defaultConfig || {};

    this.handleConfigChange({ ...defaultConfig, type: nextType });
  };

  handleTestTrigger = () => {
    const { notification, onTest } = this.props;

    onTest(notification);
  };

  formattedEventNotificationTypes = () => {
    return PluginStore.exports('eventNotificationTypes')
      .map((type) => ({ label: type.displayName, value: type.type }));
  };

  render() {
    const { action, embedded, formId, notification, onCancel, validation, testResult } = this.props;

    const notificationPlugin = this.getNotificationPlugin(notification.config.type);
    const notificationFormComponent = notificationPlugin.formComponent
      ? React.createElement(notificationPlugin.formComponent, {
        config: notification.config,
        onChange: this.handleConfigChange,
        validation: validation,
      })
      : null;

    const testButtonText = testResult.isLoading ? <Spinner text="Testing..." /> : 'Execute Test Notification';

    return (
      <Row>
        <Col md={12}>
          <form onSubmit={this.handleSubmit} id={formId}>
            <Input id="notification-title"
                   name="title"
                   label="Title"
                   type="text"
                   bsStyle={validation.errors.title ? 'error' : null}
                   help={lodash.get(validation, 'errors.title[0]', 'Title to identify this Notification.')}
                   value={notification.title}
                   onChange={this.handleChange}
                   required />

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
                      options={this.formattedEventNotificationTypes()}
                      value={notification.config.type}
                      onChange={this.handleTypeChange}
                      clearable={false}
                      required />
              <HelpBlock>
                {lodash.get(validation, 'errors.config[0]', 'Choose the type of Notification to create.')}
              </HelpBlock>
            </FormGroup>

            {notificationFormComponent}

            <FormGroup>
              <ControlLabel>Test Notification <small className="text-muted">(Optional)</small></ControlLabel>
              <FormControl.Static>
                <Button bsStyle="info" bsSize="small" disabled={testResult.isLoading} onClick={this.handleTestTrigger}>
                  {testButtonText}
                </Button>
              </FormControl.Static>
              {testResult.message && (
                <Alert bsStyle={testResult.error ? 'danger' : 'success'}>
                  <b>{testResult.error ? 'Error: ' : 'Success: '}</b>
                  {testResult.message}
                </Alert>
              )}
              <HelpBlock>
                Execute this Notification with a test Alert.
              </HelpBlock>
            </FormGroup>

            {!embedded && (
              <ButtonToolbar>
                <Button bsStyle="primary" type="submit">{action === 'create' ? 'Create' : 'Update'}</Button>
                <Button onClick={onCancel}>Cancel</Button>
              </ButtonToolbar>
            )}
          </form>
        </Col>
      </Row>
    );
  }
}

export default EventNotificationForm;
