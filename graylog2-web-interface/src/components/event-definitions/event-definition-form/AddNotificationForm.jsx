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

import { Select } from 'components/common';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import EventNotificationFormContainer
  from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

import commonStyles from '../common/commonStyles.css';

class AddNotificationForm extends React.Component {
  static propTypes = {
    notifications: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    hasCreationPermissions: PropTypes.bool,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  };

  static defaultProps = {
    hasCreationPermissions: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      selectedNotification: undefined,
      displayNewNotificationForm: false,
    };
  }

  handleNewNotificationSubmit = (promise) => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.DONE_CLICKED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-notifications',
      app_action_value: 'done-button',
    });

    const { onChange } = this.props;

    promise.then((notification) => onChange(notification.id));
  };

  handleSubmit = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.DONE_CLICKED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-notifications',
      app_action_value: 'done-button',
    });

    const { onChange } = this.props;
    const { selectedNotification } = this.state;

    onChange(selectedNotification);
  };

  handleCancel = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.CANCEL_CLICKED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-notifications',
      app_action_value: 'cancel-button',
    });

    this.props.onCancel();
  };

  handleSelectNotificationChange = (nextNotificationId) => {
    if (nextNotificationId === 'create') {
      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.CREATE_NEW_CLICKED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_section: 'event-definition-notifications',
        app_action_value: 'create-new-option',
      });

      this.setState({ displayNewNotificationForm: true });

      return;
    }

    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.NOTIFICATION_SELECTED, {
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-notifications',
      app_action_value: 'existing-notification-option',
    });

    this.setState({ selectedNotification: nextNotificationId, displayNewNotificationForm: false });
  };

  formatNotifications = (notifications) => {
    const { hasCreationPermissions } = this.props;
    const formattedNotifications = notifications.map((n) => ({ label: n.title, value: n.id }));

    if (hasCreationPermissions) {
      formattedNotifications.unshift({
        label: 'Create New Notification...',
        value: 'create',
      });
    }

    return formattedNotifications;
  };

  render() {
    const { notifications } = this.props;
    const { displayNewNotificationForm, selectedNotification } = this.state;
    const doneButton = displayNewNotificationForm
      ? <Button bsStyle="success" type="submit" form="new-notification-form">Add notification</Button>
      : <Button bsStyle="success" onClick={this.handleSubmit}>Add notification</Button>;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>Add Notification</h2>

          <fieldset>
            <FormGroup controlId="notification-select">
              <ControlLabel>Choose Notification</ControlLabel>
              <Select id="notification-select"
                      matchProp="label"
                      placeholder="Select Notification"
                      onChange={this.handleSelectNotificationChange}
                      options={this.formatNotifications(notifications)}
                      value={selectedNotification} />
              <HelpBlock>
                Select a Notification to use on Alerts of this kind or create a new Notification that you can
                later use in other Alerts.
              </HelpBlock>
            </FormGroup>

            {displayNewNotificationForm && (
              <EventNotificationFormContainer action="create"
                                              formId="new-notification-form"
                                              onSubmit={this.handleNewNotificationSubmit}
                                              embedded />
            )}
          </fieldset>

          <ButtonToolbar>
            {doneButton}
            <Button onClick={this.handleCancel}>Cancel</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default withLocation(withTelemetry(AddNotificationForm));
