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
import 'webpack-entry';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'aws/common/Routes';

import AWSInputConfiguration from './aws/AWSInputConfiguration';
import AWSCloudWatchApp from './aws/cloudwatch/CloudWatchApp';
import EmbeddedCloudWatchApp from './aws/cloudwatch/EmbeddedCloudWatchApp';
import PagerDutyNotificationDetails from './pager-duty/PagerDutyNotificationDetails';
import PagerDutyNotificationForm from './pager-duty/PagerDutyNotificationForm';
import PagerDutyNotificationSummary from './pager-duty/PagerDutyNotificationSummary';
import SlackNotificationDetails from './event-notifications/event-notification-details/SlackNotificationDetails';
import SlackNotificationForm from './event-notifications/event-notification-types/SlackNotificationForm';
import SlackNotificationSummary from './event-notifications/event-notification-types/SlackNotificationSummary';

import packageJson from '../../package.json';
import GreyNoiseAdapterFieldSet from "./dataadapters/GreyNoiseAdapterFieldSet";
import GreyNoiseAdapterSummary from "./dataadapters/GreyNoiseAdapterSummary";
import GreyNoiseAdapterDocumentation from "./dataadapters/GreyNoiseAdapterDocumentation";

const manifest = new PluginManifest(packageJson, {
  routes: [
    { path: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index, component: AWSCloudWatchApp },
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
  ],
  lookupTableAdapters: [
    {
      type: 'GreyNoise',
      displayName: 'GreyNoise',
      formComponent: GreyNoiseAdapterFieldSet,
      summaryComponent: GreyNoiseAdapterSummary,
      documentationComponent: GreyNoiseAdapterDocumentation,
    }],
});

PluginStore.register(manifest);
