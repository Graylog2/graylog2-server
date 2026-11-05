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

import type { SlackNotificationSummaryType } from 'integrations/event-notifications/types';
import CommonNotificationSummary from 'components/event-notifications/event-notification-types/CommonNotificationSummary';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

function SlackNotificationSummary({ notification, ...restProps }: SlackNotificationSummaryType) {
  const { t } = useTranslation('integrations');

  return (
    <CommonNotificationSummary {...restProps} notification={notification}>
      <tr>
        <td>{t('slack_notification_summary.color_label')}</td>
        <td>{notification?.config?.color}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.webhook_url_label')}</td>
        <td>{notification.config.webhook_url}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.channel_label')}</td>
        <td>{notification.config.channel}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.include_title_label')}</td>
        <td>{notification.config.include_title}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_summary.custom_message_label')}</td>
        <td>{notification.config.custom_message}</td>
      </tr>
      {usePluggableLicenseCheck('/license/security')?.data?.valid && (
        <tr>
          <td>{t('slack_notification_details.include_event_procedure_label')}</td>
          <td>{notification.config.include_event_procedure}</td>
        </tr>
      )}
      <tr>
        <td>{t('slack_notification_details.time_zone_label')}</td>
        <td>{notification.config.time_zone}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.backlog_limit_label')}</td>
        <td>{notification.config.backlog_size}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.user_name_label')}</td>
        <td>{notification.config.user_name}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.notify_channel_label')}</td>
        <td>
          {notification.config.notify_channel
            ? t('slack_notification_summary.yes')
            : t('slack_notification_summary.no')}
        </td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.link_names_label')}</td>
        <td>
          {notification.config.link_names ? t('slack_notification_summary.yes') : t('slack_notification_summary.no')}
        </td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.icon_url_label')}</td>
        <td>{notification.config.icon_url}</td>
      </tr>
      <tr>
        <td>{t('slack_notification_details.icon_emoji_label')}</td>
        <td>{notification.config.icon_emoji}</td>
      </tr>
    </CommonNotificationSummary>
  );
}

export default SlackNotificationSummary;
