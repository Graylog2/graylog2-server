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

import i18n from 'i18n';
import { Select } from 'components/common';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import commonStyles from '../common/commonStyles.css';

const t = (key: string) => i18n.t(key, { ns: 'event_definitions' });

type AddNotificationFormProps = {
  notifications: any[];
  onChange: (...args: any[]) => void;
  onCancel: (...args: any[]) => void;
  hasCreationPermissions?: boolean;
  sendTelemetry: (...args: any[]) => void;
};

class AddNotificationForm extends React.Component<
  AddNotificationFormProps,
  {
    [key: string]: any;
  }
> {
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
      app_action_value: 'done-button',
    });

    const { onChange } = this.props;

    promise.then((notification) => onChange(notification.id));
  };

  handleSubmit = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.DONE_CLICKED, {
      app_action_value: 'done-button',
    });

    const { onChange } = this.props;
    const { selectedNotification } = this.state;

    onChange(selectedNotification);
  };

  handleCancel = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.CANCEL_CLICKED, {
      app_action_value: 'cancel-button',
    });

    this.props.onCancel();
  };

  handleSelectNotificationChange = (nextNotificationId) => {
    if (nextNotificationId === 'create') {
      this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.CREATE_NEW_CLICKED, {
        app_action_value: 'create-new-option',
      });

      this.setState({ displayNewNotificationForm: true });

      return;
    }

    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.NOTIFICATION_SELECTED, {
      app_action_value: 'existing-notification-option',
    });

    this.setState({ selectedNotification: nextNotificationId, displayNewNotificationForm: false });
  };

  formatNotifications = (notifications) => {
    const { hasCreationPermissions } = this.props;
    const formattedNotifications = notifications.map((n) => ({ label: n.title, value: n.id }));

    if (hasCreationPermissions) {
      formattedNotifications.unshift({
        label: t('add_notification_form.create_new'),
        value: 'create',
      });
    }

    return formattedNotifications;
  };

  render() {
    const { notifications } = this.props;
    const { displayNewNotificationForm, selectedNotification } = this.state;
    const doneButton = displayNewNotificationForm ? (
      <Button bsStyle="primary" type="submit" form="new-notification-form">
        {t('add_notification_form.add')}
      </Button>
    ) : (
      <Button bsStyle="primary" onClick={this.handleSubmit} disabled={!selectedNotification}>
        {t('add_notification_form.add')}
      </Button>
    );

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>{t('add_notification_form.title')}</h2>

          <fieldset>
            <FormGroup controlId="notification-select">
              <ControlLabel>{t('add_notification_form.choose')}</ControlLabel>
              <Select
                id="notification-select"
                placeholder={t('add_notification_form.select')}
                onChange={this.handleSelectNotificationChange}
                options={this.formatNotifications(notifications)}
                value={selectedNotification}
                menuPlacement="bottom"
              />
              <HelpBlock>{t('add_notification_form.help')}</HelpBlock>
            </FormGroup>

            {displayNewNotificationForm && (
              <EventNotificationFormContainer
                action="create"
                formId="new-notification-form"
                onSubmit={this.handleNewNotificationSubmit}
                embedded
              />
            )}
          </fieldset>

          <ButtonToolbar>
            {doneButton}
            <Button onClick={this.handleCancel}>{i18n.t('common:actions.cancel')}</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default withTelemetry(AddNotificationForm, 'event-definition-notifications');
