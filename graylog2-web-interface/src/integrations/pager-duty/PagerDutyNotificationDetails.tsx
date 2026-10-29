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

import { ReadOnlyFormGroup } from 'components/common';

import type { PagerDutyConfig } from './PagerDutyConfig';

type PagerDutyNotificationDetailsProps = {
  notification: {
    config: PagerDutyConfig;
  };
};

const PagerDutyNotificationDetails = ({ notification }: PagerDutyNotificationDetailsProps) => {
  const { t } = useTranslation('integrations');

  return (
    <>
      <ReadOnlyFormGroup
        label={t('pager_duty_notification_details.routing_key_label')}
        value={notification.config?.routing_key}
      />
      <ReadOnlyFormGroup
        label={t('pager_duty_notification_details.incident_title_label')}
        value={notification.config?.pager_duty_title}
      />
      <ReadOnlyFormGroup
        label={t('pager_duty_notification_details.custom_incident_label')}
        value={notification.config?.custom_incident}
      />
      {notification?.config?.custom_incident && notification.config?.key_prefix && (
        <ReadOnlyFormGroup
          label={t('pager_duty_notification_details.key_prefix_label')}
          value={notification.config?.key_prefix}
        />
      )}
      {notification?.config?.custom_incident && notification.config?.incident_key && (
        <ReadOnlyFormGroup
          label={t('pager_duty_notification_details.incident_key_label')}
          value={notification.config?.incident_key}
        />
      )}
      <ReadOnlyFormGroup
        label={t('pager_duty_notification_details.client_name_label')}
        value={notification.config?.client_name}
      />
      <ReadOnlyFormGroup
        label={t('pager_duty_notification_details.client_url_label')}
        value={notification.config?.client_url}
      />
    </>
  );
};

export default PagerDutyNotificationDetails;
