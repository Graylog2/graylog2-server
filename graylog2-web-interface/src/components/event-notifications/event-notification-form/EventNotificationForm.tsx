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
import * as React from 'react';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { FormSubmit, Select, Spinner } from 'components/common';
import { Alert, Button, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import { getValueFromInput } from 'util/FormsUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';
import notificationTypeTitle from 'components/event-notifications/notificationTypeTitle';

const getNotificationPlugin = (type: string) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type);
};

const formattedEventNotificationTypes = () =>
  PluginStore.exports('eventNotificationTypes').map((type) => ({ label: notificationTypeTitle(type), value: type.type }));

type EventNotificationFormProps = {
  action?: 'create' | 'edit';
  notification: EventNotification;
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
};

const EventNotificationForm = ({
  action = 'edit',
  embedded,
  formId = undefined,
  notification,
  onCancel,
  validation,
  testResult,
  onSubmit,
  onChange,
  onTest,
}: EventNotificationFormProps) => {
  const { t } = useTranslation('events');
  const sendTelemetry = useSendTelemetry();
  const [isSubmitEnabled, setIsSubmitEnabled] = useState(true);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    sendTelemetry(
      action === 'create'
        ? TELEMETRY_EVENT_TYPE.NOTIFICATIONS.CREATE_CLICKED
        : TELEMETRY_EVENT_TYPE.NOTIFICATIONS.EDIT_CLICKED,
      {
        app_section: 'event-notification',
        app_action_value: `${action}-button`,
      },
    );

    event.preventDefault();

    onSubmit(notification);
  };

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;

    onChange(name, getValueFromInput(event.target));
  };

  const handleConfigChange = (nextConfig: EventNotification['config']) => {
    onChange('config', nextConfig);
  };

  const handleEntityShareChange = (entityShare: EntitySharePayload) => {
    onChange('share_request', entityShare);
  };

  const handleTypeChange = (nextType: string) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.NOTIFICATION_TYPE_SELECTED, {
      app_section: 'event-definition-notifications',
      app_action_value: 'notification-type-select',
      notification_type: nextType,
    });

    const notificationPlugin = getNotificationPlugin(nextType);
    const defaultConfig = notificationPlugin?.defaultConfig || {};

    handleConfigChange({ ...defaultConfig, type: nextType });
  };

  const handleTestTrigger = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.NOTIFICATIONS.EXECUTE_TEST_CLICKED, {
      app_section: 'event-notification',
      app_action_value: 'execute-test-button',
    });

    onTest(notification);
  };

  const notificationPlugin = getNotificationPlugin(notification.config.type);
  const notificationFormComponent = notificationPlugin?.formComponent
    ? React.createElement(notificationPlugin.formComponent, {
        config: notification.config,
        onChange: handleConfigChange,
        validation: validation,
        setIsSubmitEnabled: setIsSubmitEnabled,
      })
    : null;

  const isNew = action === 'create';
  const testButtonText = testResult.isLoading ? (
    <Spinner text={t('notifications_form.testing')} />
  ) : (
    t('notifications_form.execute_test')
  );

  return (
    <Row>
      <Col lg={8}>
        <form onSubmit={handleSubmit} id={formId}>
          <Input
            id="notification-title"
            name="title"
            label={t('notifications_form.title')}
            type="text"
            bsStyle={validation.errors.title ? 'error' : null}
            help={validation?.errors?.title?.[0] ?? t('notifications_form.title_help')}
            value={notification.title}
            onChange={handleChange}
            required
            autoFocus
          />

          <Input
            id="notification-description"
            name="description"
            label={
              <span>
                {t('notifications_form.description')}{' '}
                <small className="text-muted">{t('notifications_form.optional')}</small>
              </span>
            }
            type="textarea"
            help={t('notifications_form.description_help')}
            value={notification.description}
            onChange={handleChange}
            rows={2}
          />

          <FormGroup controlId="notification-type" validationState={validation.errors.config ? 'error' : null}>
            <ControlLabel>{t('notifications_form.type')}</ControlLabel>
            <Select
              id="notification-type"
              options={formattedEventNotificationTypes()}
              value={notification.config.type}
              onChange={handleTypeChange}
              clearable={false}
              required
            />
            <HelpBlock>{validation?.errors?.config?.[0] ?? t('notifications_form.type_help')}</HelpBlock>
          </FormGroup>

          {notificationFormComponent}

          {notificationFormComponent && (
            <FormGroup>
              <ControlLabel>
                {t('notifications_form.test')} <small className="text-muted">{t('notifications_form.optional')}</small>
              </ControlLabel>
              <FormControl.Static>
                <Button bsStyle="info" bsSize="small" disabled={testResult.isLoading} onClick={handleTestTrigger}>
                  {testButtonText}
                </Button>
              </FormControl.Static>
              {testResult.message && (
                <Alert
                  bsStyle={testResult.error ? 'danger' : 'success'}
                  title={testResult.error ? t('notifications_form.error_prefix') : t('notifications_form.success_prefix')}>
                  {testResult.message}
                </Alert>
              )}
              <HelpBlock>{t('notifications_form.test_help')}</HelpBlock>
            </FormGroup>
          )}
          {isNew && (
            <EntityCreateShareFormGroup
              description={t('notifications_form.share_description')}
              entityType="notification"
              entityTitle=""
              onSetEntityShare={handleEntityShareChange}
            />
          )}

          {!embedded && (
            <FormSubmit
              disabledSubmit={!isSubmitEnabled}
              submitButtonText={isNew ? t('notifications_form.create') : t('notifications_form.update')}
              onCancel={onCancel}
            />
          )}
        </form>
      </Col>
    </Row>
  );
};

export default EventNotificationForm;
