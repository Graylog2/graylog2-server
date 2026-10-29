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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import type { SlackNotificationSummaryType } from 'integrations/event-notifications/types';
import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/bootstrap';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

const NewExampleWell = styled(Well)(
  ({ theme }) => css`
    margin-bottom: 5px;
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.body};
    white-space: pre-wrap;
    overflow-wrap: break-word;
  `,
);

const SlackNotificationDetails = ({ notification }: SlackNotificationSummaryType) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <ReadOnlyFormGroup label={t('slack_notification_details.webhook_url_label')} value={notification.config.webhook_url} />
      <ReadOnlyFormGroup label={t('slack_notification_details.channel_label')} value={notification.config.channel} />
      <ReadOnlyFormGroup
        label={t('slack_notification_details.custom_message_label')}
        value={
          <NewExampleWell bsSize="small">
            {notification.config.custom_message || <em>{t('slack_notification_details.empty_body')}</em>}
          </NewExampleWell>
        }
      />
      {usePluggableLicenseCheck('/license/security')?.data?.valid && (
        <ReadOnlyFormGroup
          label={t('slack_notification_details.include_event_procedure_label')}
          value={notification.config.include_event_procedure}
        />
      )}
      <ReadOnlyFormGroup
        label={t('slack_notification_details.backlog_limit_label')}
        value={notification.config.backlog_size}
      />
      <ReadOnlyFormGroup label={t('slack_notification_details.user_name_label')} value={notification.config.user_name} />
      <ReadOnlyFormGroup
        label={t('slack_notification_details.include_title_label')}
        value={notification.config.include_title}
      />
      <ReadOnlyFormGroup
        label={t('slack_notification_details.notify_channel_label')}
        value={notification.config.notify_channel}
      />
      <ReadOnlyFormGroup label={t('slack_notification_details.link_names_label')} value={notification.config.link_names} />
      <ReadOnlyFormGroup label={t('slack_notification_details.icon_url_label')} value={notification.config.icon_url} />
      <ReadOnlyFormGroup label={t('slack_notification_details.icon_emoji_label')} value={notification.config.icon_emoji} />
      <ReadOnlyFormGroup label={t('slack_notification_details.time_zone_label')} value={notification.config.time_zone} />
    </>
  );
};

export default SlackNotificationDetails;
