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
import type { ReactElement } from 'react';
import type { PluginExports } from 'graylog-web-plugin/plugin';
import { Trans, useTranslation } from 'react-i18next';

import i18n from 'i18n';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import type { IsPermitted } from 'hooks/usePermissions';
import { Alert } from 'components/bootstrap';
import { MarkdownPreview } from 'components/common/MarkdownEditor';
import type User from 'logic/users/User';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';

import notificationTypeTitle from 'components/event-notifications/notificationTypeTitle';

import commonStyles from '../common/commonStyles.css';
import type { EventDefinition } from '../event-definitions-types';
import { eventDefinitionTypeTitle } from '../pluginTitles';
import formatDuration from '../formatDuration';

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
  const { t } = useTranslation('event_definitions');
  const pluggableEventProcedureSummary = usePluginEntities('views.components.eventProcedureSummary');
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  let label = null;
  let Component: ReactElement | null = null;

  if (validSecurityLicense) {
    label = t('summary_renders.event_procedure_summary');

    if (eventProcedureId) {
      const first = pluggableEventProcedureSummary?.[0];
      const PluggableEventProcedureSummary = first?.component;

      Component = PluggableEventProcedureSummary ? (
        <PluggableEventProcedureSummary eventProcedureId={eventProcedureId} key={first?.key} />
      ) : (
        <p>{t('summary_renders.event_procedure_summary_unavailable')}</p>
      );
    } else {
      Component = <p>{t('summary_renders.no_event_procedures')}</p>;
    }
  } else {
    label = t('summary_renders.remediation_steps');
    Component = (
      <MarkdownPreview
        show
        withFullView
        noBorder
        noBackground
        value={remediationSteps || t('summary_renders.no_remediation_steps')}
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
      <Trans
        i18n={i18n}
        ns="event_definitions"
        i18nKey="summary_renders.condition_plugin_no_summary"
        values={{ type: config.type }}
        components={{ em: <em /> }}
      />
    </p>
  );

  return (
    <>
      <h3 className={commonStyles.title}>{eventDefinitionTypeTitle(conditionPlugin) || config.type}</h3>
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
    return <span key={fieldName}>{i18n.t('summary_renders.no_field_value_provider', { ns: 'event_definitions' })}</span>;
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
      <Trans
        i18n={i18n}
        ns="event_definitions"
        i18nKey="summary_renders.provider_plugin_no_summary"
        values={{ type: provider.type }}
        components={{ em: <em /> }}
      />
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
        <dt>{i18n.t('summary_renders.keys', { ns: 'event_definitions' })}</dt>
        <dd>
          {keys.length > 0 ? keys.join(', ') : i18n.t('summary_renders.no_keys', { ns: 'event_definitions' })}
        </dd>
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
      <h3 className={commonStyles.title}>{i18n.t('summary_renders.fields', { ns: 'event_definitions' })}</h3>
      {fieldNames.length === 0 ? (
        <p>{i18n.t('summary_renders.no_fields', { ns: 'event_definitions' })}</p>
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
        type: notificationTypeTitle(notificationPlugin),
        notification,
        definitionNotification,
      })
    ) : (
      <p>
        <Trans
          i18n={i18n}
          ns="event_definitions"
          i18nKey="summary_renders.notification_plugin_no_summary"
          values={{ type: notification.config.type }}
          components={{ em: <em /> }}
        />
      </p>
    );
  } else {
    content = (
      <p>
        <Trans
          i18n={i18n}
          ns="event_definitions"
          i18nKey="summary_renders.notification_not_found"
          values={{ id: definitionNotification.notification_id }}
          components={{ em: <em /> }}
        />
      </p>
    );
  }

  return <React.Fragment key={definitionNotification.notification_id}>{content}</React.Fragment>;
}

function renderNotificationSettings(notificationSettings: { grace_period_ms: number; backlog_size: number | null }) {
  const formattedDuration = formatDuration(notificationSettings.grace_period_ms);

  const formattedGracePeriod = notificationSettings.grace_period_ms
    ? i18n.t('summary_renders.grace_period_set', { ns: 'event_definitions', duration: formattedDuration })
    : i18n.t('summary_renders.grace_period_disabled', { ns: 'event_definitions' });

  const formattedBacklogSize = notificationSettings.backlog_size
    ? i18n.t('summary_renders.backlog_included', { ns: 'event_definitions', count: notificationSettings.backlog_size })
    : i18n.t('summary_renders.backlog_none', { ns: 'event_definitions' });

  return (
    <>
      <h4>{i18n.t('summary_renders.settings', { ns: 'event_definitions' })}</h4>
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
  isPermitted: IsPermitted,
) {
  const effectiveDefinitionNotifications = definitionNotifications.filter((n) =>
    isPermitted(`eventnotifications:read:${n.notification_id}`),
  );

  const notificationsWithMissingPermissions = definitionNotifications.filter(
    (n) => !effectiveDefinitionNotifications.map((nObj) => nObj.notification_id).includes(n.notification_id),
  );

  const warning =
    notificationsWithMissingPermissions.length > 0 ? (
      <Alert bsStyle="warning">
        {i18n.t('summary_renders.missing_notification_permissions', { ns: 'event_definitions' })}
        <br />
        {notificationsWithMissingPermissions.map((n) => n.notification_id).join(', ')}
      </Alert>
    ) : null;

  return (
    <>
      <h3 className={commonStyles.title}>{i18n.t('summary_renders.notifications', { ns: 'event_definitions' })}</h3>
      <span>{warning}</span>
      {effectiveDefinitionNotifications.length === 0 && notificationsWithMissingPermissions.length <= 0 ? (
        <p>{i18n.t('summary_renders.no_notifications', { ns: 'event_definitions' })}</p>
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
