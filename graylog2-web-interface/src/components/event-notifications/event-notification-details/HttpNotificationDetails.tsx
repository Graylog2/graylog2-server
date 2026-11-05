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

type HttpNotificationDetailsProps = {
  notification: HttpEventNotificationV2;
};

const HttpNotificationDetails = ({ notification }: HttpNotificationDetailsProps) => {
  const { t } = useTranslation('event_notifications');
  const apiKeySet = notification.config.api_secret?.is_set;
  const apiSentAs = notification.config.api_key_as_header
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
    </>
  );
};

export default HttpNotificationDetails;
