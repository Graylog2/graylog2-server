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
import { PluginStore } from 'graylog-web-plugin/plugin';
import moment from 'moment';
import 'moment-duration-format';
import type { ReactElement } from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import { Alert } from 'components/bootstrap';
import { isPermitted } from 'util/PermissionsMixin';
import { MarkdownPreview } from 'components/common/MarkdownEditor';
import type User from 'logic/users/User';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';

import commonStyles from '../common/commonStyles.css';
import type { EventDefinition } from '../event-definitions-types';

type EventProcedureSummaryComponentsType = {
  eventProcedureId?: string;
  remediationSteps?: string;
};

const getPlugin = <T extends 'eventDefinitionTypes' | 'fieldValueProviders' | 'eventNotificationTypes'>(
  name: T,
  type: string,
): PluginExports[T][number] => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports(name).find((edt) => edt.type === type);
};

export function useEventProcedureSummaryComponents({
  eventProcedureId = undefined,
  remediationSteps = undefined,
}: EventProcedureSummaryComponentsType) {
  const pluggableEventProcedureSummary = usePluginEntities('views.components.eventProcedureSummary');
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  let label = null;
  let Component: ReactElement | null = null;

  if (validSecurityLicense) {
    label = 'Event Procedure Summary';

    if (eventProcedureId) {
      const first = pluggableEventProcedureSummary?.[0];
      const PluggableEventProcedureSummary = first?.component;

      Component = PluggableEventProcedureSummary ? (
        <PluggableEventProcedureSummary eventProcedureId={eventProcedureId} key={first?.key} />
      ) : (
        <p>Event Procedure Summary component is not available.</p>
      );
    } else {
      Component = <p>This Event does not have any Event Procedures.</p>;
    }
  } else {
    label = 'Remediation Steps';
    Component = (
      <MarkdownPreview
        show
        withFullView
        noBorder
        noBackground
        value={remediationSteps || 'No remediation steps given'}
      />
    );
  }

  return {
    label,
    Component,
  };
}

export function renderCondition(eventDefinition: EventDefinition, definitionId: string, currentUser: User) {
  const { config, _scope } = eventDefinition;
  const conditionPlugin = getPlugin('eventDefinitionTypes', config.type);
  const component = conditionPlugin?.summaryComponent ? (
    React.createElement(conditionPlugin.summaryComponent, {
      config,
      currentUser,
      definitionId,
      entityScope: _scope,
    })
  ) : (
    <p>
      Condition plugin <em>{config.type}</em> does not provide a summary.
    </p>
  );

  return (
    <>
      <h3 className={commonStyles.title}>{conditionPlugin?.displayName || config.type}</h3>
      {component}
    </>
  );
}

export function renderField(
  fieldName: string,
  config: EventDefinition['field_spec'][string],
  keys: string[],
  currentUser: User,
) {
  if (!config.providers || config.providers.length === 0) {
    return <span key={fieldName}>No field value provider configured.</span>;
  }

  const provider = config.providers[0];
  const fieldProviderPlugin = getPlugin('fieldValueProviders', provider?.type);

  return fieldProviderPlugin?.summaryComponent ? (
    React.createElement(fieldProviderPlugin.summaryComponent, {
      fieldName,
      config,
      keys,
      key: fieldName,
      currentUser,
    })
  ) : (
    <p key={fieldName}>
      Provider plugin <em>{provider.type}</em> does not provide a summary.
    </p>
  );
}

function renderFieldList(
  fieldNames: string[],
  fields: EventDefinition['field_spec'],
  keys: EventDefinition['key_spec'],
  currentUser: User,
) {
  return (
    <>
      <dl>
        <dt>Keys</dt>
        <dd>{keys.length > 0 ? keys.join(', ') : 'No Keys configured for Events based on this Definition.'}</dd>
      </dl>
      {fieldNames.sort(naturalSort).map((fieldName) => renderField(fieldName, fields[fieldName], keys, currentUser))}
    </>
  );
}

export function renderFields(
  fields: EventDefinition['field_spec'],
  keys: EventDefinition['key_spec'],
  currentUser: User,
) {
  const fieldNames = Object.keys(fields);

  return (
    <>
      <h3 className={commonStyles.title}>Fields</h3>
      {fieldNames.length === 0 ? (
        <p>No Fields configured for Events based on this Definition.</p>
      ) : (
        renderFieldList(fieldNames, fields, keys, currentUser)
      )}
    </>
  );
}

function renderNotification(
  definitionNotification: EventDefinition['notifications'][number],
  notifications: Array<EventNotification>,
) {
  const notification = notifications.find((n) => n.id === definitionNotification.notification_id);

  let content: ReactElement = null;

  if (notification) {
    const notificationPlugin = getPlugin('eventNotificationTypes', notification.config.type);

    content = notificationPlugin.summaryComponent ? (
      React.createElement(notificationPlugin.summaryComponent, {
        type: notificationPlugin.displayName,
        notification,
        definitionNotification,
      })
    ) : (
      <p>
        Notification plugin <em>{notification.config.type}</em> does not provide a summary.
      </p>
    );
  } else {
    content = (
      <p>
        Could not find information for Notification <em>{definitionNotification.notification_id}</em>.
      </p>
    );
  }

  return <React.Fragment key={definitionNotification.notification_id}>{content}</React.Fragment>;
}

function renderNotificationSettings(notificationSettings: { grace_period_ms: number; backlog_size: number | null }) {
  const formattedDuration = moment
    .duration(notificationSettings.grace_period_ms)
    .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all' });

  const formattedGracePeriod = notificationSettings.grace_period_ms
    ? `Grace Period is set to ${formattedDuration}`
    : 'Grace Period is disabled';

  const formattedBacklogSize = notificationSettings.backlog_size
    ? `Notifications will include ${notificationSettings.backlog_size} messages`
    : 'Notifications will not include any messages.';

  return (
    <>
      <h4>Settings</h4>
      <dl>
        <dd>{formattedGracePeriod}</dd>
        <dd>{formattedBacklogSize}</dd>
      </dl>
    </>
  );
}

export function renderNotifications(
  definitionNotifications: EventDefinition['notifications'],
  notificationSettings: EventDefinition['notification_settings'],
  notifications: Array<EventNotification>,
  currentUser: User,
) {
  const effectiveDefinitionNotifications = definitionNotifications.filter((n) =>
    isPermitted(currentUser.permissions, `eventnotifications:read:${n.notification_id}`),
  );

  const notificationsWithMissingPermissions = definitionNotifications.filter(
    (n) => !effectiveDefinitionNotifications.map((nObj) => nObj.notification_id).includes(n.notification_id),
  );

  const warning =
    notificationsWithMissingPermissions.length > 0 ? (
      <Alert bsStyle="warning">
        Missing Notifications Permissions for:
        <br />
        {notificationsWithMissingPermissions.map((n) => n.notification_id).join(', ')}
      </Alert>
    ) : null;

  return (
    <>
      <h3 className={commonStyles.title}>Notifications</h3>
      <span>{warning}</span>
      {effectiveDefinitionNotifications.length === 0 && notificationsWithMissingPermissions.length <= 0 ? (
        <p>This Event is not configured to trigger any Notifications.</p>
      ) : (
        <>
          {renderNotificationSettings(notificationSettings)}
          {definitionNotifications.map((notification) => renderNotification(notification, notifications))}
        </>
      )}
    </>
  );
}

export function useTechniquesSummary(eventDefinition: EventDefinition) {
  const tacticsTechniquesSummaryPlugin = usePluginEntities('eventDefinitions.components.tacticsTechniquesSummary')[0];
  const tacticsTechniquesSummaryEnabled =
    tacticsTechniquesSummaryPlugin?.useCondition?.() ?? !!tacticsTechniquesSummaryPlugin;

  return tacticsTechniquesSummaryEnabled && tacticsTechniquesSummaryPlugin ? (
    <tacticsTechniquesSummaryPlugin.component eventDefinition={eventDefinition} />
  ) : null;
}
