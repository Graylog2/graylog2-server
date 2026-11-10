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

import { Spinner } from 'components/common';
import { useLegacyEventNotificationTypes } from 'components/event-notifications/hooks/useEventNotifications';
import type { EventNotificationTypes } from 'components/event-notifications/types';

import LegacyNotificationSummary from './LegacyNotificationSummary';

type LegacyNotificationSummaryContainerProps = React.ComponentProps<EventNotificationTypes['summaryComponent']>;

const LegacyNotificationSummaryContainer = ({ ...props }: LegacyNotificationSummaryContainerProps) => {
  const { data } = useLegacyEventNotificationTypes();
  const allLegacyTypes = data?.types;

  if (!allLegacyTypes) {
    return (
      <p>
        <Spinner text="Loading legacy notification information..." />
      </p>
    );
  }

  return <LegacyNotificationSummary {...props} legacyTypes={allLegacyTypes} />;
};

export default LegacyNotificationSummaryContainer;
