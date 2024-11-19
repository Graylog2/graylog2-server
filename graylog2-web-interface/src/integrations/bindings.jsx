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
import Routes from 'integrations/aws/common/Routes';

import AWSInputConfiguration from './aws/AWSInputConfiguration';
import AWSCloudWatchApp from './aws/cloudwatch/CloudWatchApp';
import EmbeddedCloudWatchApp from './aws/cloudwatch/EmbeddedCloudWatchApp';
import PagerDutyNotificationDetails from './pager-duty/PagerDutyNotificationDetails';
import PagerDutyNotificationForm from './pager-duty/PagerDutyNotificationForm';
import PagerDutyNotificationSummary from './pager-duty/PagerDutyNotificationSummary';
import SlackNotificationDetails from './event-notifications/event-notification-details/SlackNotificationDetails';
import SlackNotificationForm from './event-notifications/event-notification-types/SlackNotificationForm';
import SlackNotificationSummary from './event-notifications/event-notification-types/SlackNotificationSummary';
import TeamsNotificationDetails from './event-notifications/event-notification-details/TeamsNotificationDetails';
import TeamsNotificationForm from './event-notifications/event-notification-types/TeamsNotificationForm';
import TeamsNotificationSummary from './event-notifications/event-notification-types/TeamsNotificationSummary';
import GreyNoiseAdapterFieldSet from './dataadapters/GreyNoiseAdapterFieldSet';
import GreyNoiseAdapterSummary from './dataadapters/GreyNoiseAdapterSummary';
import GreyNoiseAdapterDocumentation from './dataadapters/GreyNoiseAdapterDocumentation';
import TeamsNotificationV2Form from './event-notifications/event-notification-types/TeamsNotificationV2Form';
import TeamsNotificationV2Summary from './event-notifications/event-notification-types/TeamsNotificationV2Summary';
import TeamsNotificationV2Details from './event-notifications/event-notification-details/TeamsNotificationV2Details';

const bindings = {
  routes: [
    { path: Routes.unqualified.INTEGRATIONS.AWS.CLOUDWATCH.index, component: AWSCloudWatchApp },
  ],
  inputConfiguration: [
    {
      type: 'org.graylog.integrations.aws.inputs.AWSInput',
      component: AWSInputConfiguration,
      embeddedComponent: EmbeddedCloudWatchApp,
    },
  ],
  eventNotificationTypes: [
    {
      type: 'pagerduty-notification-v2',
      displayName: 'PagerDuty Notification [Official]',
      formComponent: PagerDutyNotificationForm,
      summaryComponent: PagerDutyNotificationSummary,
      detailsComponent: PagerDutyNotificationDetails,
      defaultConfig: PagerDutyNotificationForm.defaultConfig,
    },
    {
      type: 'slack-notification-v1',
      displayName: 'Slack Notification',
      formComponent: SlackNotificationForm,
      summaryComponent: SlackNotificationSummary,
      detailsComponent: SlackNotificationDetails,
      defaultConfig: SlackNotificationForm.defaultConfig,
    },
    {
      type: 'teams-notification-v1',
      displayName: 'Microsoft Teams Notification (Deprecated - use V2)',
      formComponent: TeamsNotificationForm,
      summaryComponent: TeamsNotificationSummary,
      detailsComponent: TeamsNotificationDetails,
      defaultConfig: TeamsNotificationForm.defaultConfig,
    },
    {
      type: 'teams-notification-v2',
      displayName: 'Microsoft Teams Notification V2',
      formComponent: TeamsNotificationV2Form,
      summaryComponent: TeamsNotificationV2Summary,
      detailsComponent: TeamsNotificationV2Details,
      defaultConfig: TeamsNotificationV2Form.defaultConfig,
    },
  ],
  lookupTableAdapters: [
    {
      type: 'GreyNoise',
      displayName: 'GreyNoise Quick IP Lookup',
      formComponent: GreyNoiseAdapterFieldSet,
      summaryComponent: GreyNoiseAdapterSummary,
      documentationComponent: GreyNoiseAdapterDocumentation,
    },
  ],
};

export default bindings;
