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
import type { HttpEventNotificationV2 } from 'components/event-notifications/types';
import { Well } from 'components/bootstrap';

import styles from '../event-notification-types/EmailNotificationSummary.css';

type Props = {
  notification: HttpEventNotificationV2;
};

const HttpNotificationDetailsV2 = ({ notification }: Props) => {
  const { t } = useTranslation('event_notifications');
  const apiKeySet: boolean = notification.config.api_secret?.is_set;
  const apiSentAs: string = notification.config.api_key_as_header
    ? t('http_details.header')
    : t('http_details.query_parameter');

  return (
    <>
      <ReadOnlyFormGroup label={t('http_details.url')} value={notification.config.url} />
      <ReadOnlyFormGroup
        label={t('http_details.basic_auth')}
        value={notification.config.basic_auth?.is_set ? '******' : null}
      />
      <ReadOnlyFormGroup label={t('http_details.api_key_sent_as')} value={apiKeySet ? apiSentAs : null} />
      <ReadOnlyFormGroup label={t('http_details.api_key')} value={notification.config.api_key} />
      <ReadOnlyFormGroup label={t('http_details.api_secret')} value={apiKeySet ? '******' : null} />
      <ReadOnlyFormGroup label={t('http_details.method')} value={notification.config.method} />
      {notification.config.time_zone && (
        <ReadOnlyFormGroup label={t('http_details.time_zone')} value={notification.config.time_zone} />
      )}
      {notification.config.content_type && (
        <ReadOnlyFormGroup label={t('http_details.content_type')} value={notification.config.content_type} />
      )}
      {notification.config.headers && (
        <ReadOnlyFormGroup label={t('http_details.headers')} value={notification.config.headers} />
      )}
      {notification.config.body_template && (
        <ReadOnlyFormGroup
          label={t('http_details.body_template')}
          value={
            <Well bsSize="small" className={styles.bodyPreview}>
              {notification.config.body_template}
            </Well>
          }
        />
      )}
    </>
  );
};

export default HttpNotificationDetailsV2;
