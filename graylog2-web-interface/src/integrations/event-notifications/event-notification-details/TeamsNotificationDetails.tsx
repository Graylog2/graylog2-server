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

import { ReadOnlyFormGroup } from 'components/common';
import { Well } from 'components/bootstrap';

import type { TeamsNotificationSummaryType } from '../types';

const NewExampleWell = styled(Well)(
  ({ theme }) => css`
    margin-bottom: 5px;
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.body};
    white-space: pre-wrap;
    overflow-wrap: break-word;
  `,
);

const TeamsNotificationDetails = ({ notification }: TeamsNotificationSummaryType) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <ReadOnlyFormGroup label={t('teams_notification_details.webhook_url_label')} value={notification.config.webhook_url} />
      <ReadOnlyFormGroup
        label={t('teams_notification_details.custom_message_label')}
        value={
          <NewExampleWell bsSize="small">
            {notification.config.custom_message || <em>{t('teams_notification_details.empty_body')}</em>}
          </NewExampleWell>
        }
      />
      <ReadOnlyFormGroup
        label={t('teams_notification_details.backlog_limit_label')}
        value={notification.config.backlog_size}
      />
      <ReadOnlyFormGroup label={t('teams_notification_details.icon_url_label')} value={notification.config.icon_url} />
      <ReadOnlyFormGroup label={t('teams_notification_details.time_zone_label')} value={notification.config.time_zone} />
    </>
  );
};

export default TeamsNotificationDetails;
