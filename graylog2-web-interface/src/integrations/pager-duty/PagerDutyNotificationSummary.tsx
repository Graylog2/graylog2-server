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
import { useTranslation } from 'react-i18next';

import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';

import type { PagerDutyConfig } from './PagerDutyConfig';

type PagerDutyNotificationSummaryProps = {
  type: string;
  notification: {
    config: PagerDutyConfig;
  };
  definitionNotification: any;
};

function PagerDutyNotificationSummary({ notification, ...restProps }: PagerDutyNotificationSummaryProps) {
  const { t } = useTranslation('integrations');

  return (
    <CommonNotificationSummary {...restProps} notification={notification}>
      <tr>
        <td>{t('pager_duty_notification_summary.routing_key_label')}</td>
        <td>
          <code>{notification?.config?.routing_key}</code>
        </td>
      </tr>
      <tr>
        <td>{t('pager_duty_notification_summary.incident_title_label')}</td>
        <td>
          <code>{notification?.config?.pager_duty_title ?? t('pager_duty_notification_summary.default_value')}</code>
        </td>
      </tr>
      <tr>
        <td>{t('pager_duty_notification_summary.use_custom_incident_key_label')}</td>
        <td>
          <code>
            {notification?.config?.custom_incident
              ? t('pager_duty_notification_summary.yes')
              : t('pager_duty_notification_summary.no')}
          </code>
        </td>
      </tr>
      {notification?.config?.custom_incident && notification?.config?.key_prefix && (
        <tr>
          <td>{t('pager_duty_notification_summary.key_prefix_label')}</td>
          <td>
            <code>{notification?.config?.key_prefix}</code>
          </td>
        </tr>
      )}
      {notification?.config?.custom_incident && notification?.config?.incident_key && (
        <tr>
          <td>{t('pager_duty_notification_summary.incident_key_label')}</td>
          <td>
            <code>{notification?.config?.incident_key}</code>
          </td>
        </tr>
      )}
      <tr>
        <td>{t('pager_duty_notification_summary.client_name_label')}</td>
        <td>
          <code>{notification?.config?.client_name}</code>
        </td>
      </tr>
      <tr>
        <td>{t('pager_duty_notification_summary.client_url_label')}</td>
        <td>
          <code>{notification?.config?.client_url}</code>
        </td>
      </tr>
    </CommonNotificationSummary>
  );
}

export default PagerDutyNotificationSummary;
