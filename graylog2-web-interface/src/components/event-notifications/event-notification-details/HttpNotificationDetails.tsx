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

import { ReadOnlyFormGroup } from 'components/common';
import type { HttpEventNotificationV2 } from 'components/event-notifications/types';

type HttpNotificationDetailsProps = {
  notification: HttpEventNotificationV2;
};

const HttpNotificationDetails = ({
  notification,
}: HttpNotificationDetailsProps) => {
  const apiKeySet = notification.config.api_secret?.is_set;
  const apiSentAs = notification.config.api_key_as_header ? 'Header' : 'Query Parameter';

  return (
    <>
      <ReadOnlyFormGroup label="URL" value={notification.config.url} />
      <ReadOnlyFormGroup label="Basic Authentication" value={notification.config.basic_auth?.is_set ? '******' : null} />
      <ReadOnlyFormGroup label="API Key/Secret Sent As" value={apiKeySet ? apiSentAs : null} />
      <ReadOnlyFormGroup label="API Key" value={notification.config.api_key} />
      <ReadOnlyFormGroup label="API Secret" value={apiKeySet ? '******' : null} />
    </>
  );
};

export default HttpNotificationDetails;
