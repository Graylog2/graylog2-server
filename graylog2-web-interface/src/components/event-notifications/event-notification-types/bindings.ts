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
import EmailNotificationFormContainer from './EmailNotificationFormContainer';
import EmailNotificationForm from './EmailNotificationForm';
import EmailNotificationSummary from './EmailNotificationSummary';
import HttpNotificationForm from './HttpNotificationForm';
import HttpNotificationSummary from './HttpNotificationSummary';
import LegacyNotificationForm from './LegacyNotificationForm';
import LegacyNotificationFormContainer from './LegacyNotificationFormContainer';
import LegacyNotificationSummaryContainer from './LegacyNotificationSummaryContainer';
import HttpNotificationSummaryV2 from './HttpNotificationSummaryV2';
import HttpNotificationFormV2 from './HttpNotificationFormV2';

import EmailNotificationDetails from '../event-notification-details/EmailNotificationDetails';
import HttpNotificationDetails from '../event-notification-details/HttpNotificationDetails';
import LegacyNotificationDetails from '../event-notification-details/LegacyNotificationDetails';
import HttpNotificationDetailsV2 from '../event-notification-details/HttpNotificationDetailsV2';

export default {
  eventNotificationTypes: [
    {
      type: 'email-notification-v1',
      displayName: 'Email Notification',
      formComponent: EmailNotificationFormContainer,
      summaryComponent: EmailNotificationSummary,
      detailsComponent: EmailNotificationDetails,
      defaultConfig: EmailNotificationForm.defaultConfig,
    },
    {
      type: 'http-notification-v1',
      displayName: 'HTTP Notification',
      formComponent: HttpNotificationForm,
      summaryComponent: HttpNotificationSummary,
      detailsComponent: HttpNotificationDetails,
      defaultConfig: HttpNotificationForm.defaultConfig,
    },
    {
      type: 'http-notification-v2',
      displayName: 'Custom HTTP Notification',
      formComponent: HttpNotificationFormV2,
      summaryComponent: HttpNotificationSummaryV2,
      detailsComponent: HttpNotificationDetailsV2,
      defaultConfig: HttpNotificationFormV2.defaultConfig,
    },
    {
      type: 'legacy-alarm-callback-notification-v1',
      displayName: 'Legacy Alarm Callbacks',
      formComponent: LegacyNotificationFormContainer,
      summaryComponent: LegacyNotificationSummaryContainer,
      detailsComponent: LegacyNotificationDetails,
      defaultConfig: LegacyNotificationForm.defaultConfig,
    },
  ],
};
