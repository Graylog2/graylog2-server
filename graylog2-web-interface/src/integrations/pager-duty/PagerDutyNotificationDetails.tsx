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

type PagerDutyNotificationDetailsProps = {
  notification: {
    config: {
      routing_key?: string;
      custom_incident?: boolean;
      key_prefix?: string;
      client_name?: string;
      client_url?: string;
    };
  };
};

const PagerDutyNotificationDetails = ({
  notification,
}: PagerDutyNotificationDetailsProps) => (
  <>
    <ReadOnlyFormGroup label="Routing Key" value={notification.config?.routing_key} />
    <ReadOnlyFormGroup label="Custom Incident" value={notification.config?.custom_incident} />
    <ReadOnlyFormGroup label="Key Prefix" value={notification.config?.key_prefix} />
    <ReadOnlyFormGroup label="Client Name" value={notification.config?.client_name} />
    <ReadOnlyFormGroup label="Client URL" value={notification.config?.client_url} />
  </>
);

export default PagerDutyNotificationDetails;
