import 'webpack-entry';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'aws/common/Routes';

import AWSInputConfiguration from './aws/AWSInputConfiguration';
import AWSCloudWatchApp from './aws/cloudwatch/CloudWatchApp';
import PagerDutyNotificationDetails from './pager-duty/PagerDutyNotificationDetails';
import PagerDutyNotificationForm from './pager-duty/PagerDutyNotificationForm';
import PagerDutyNotificationSummary from './pager-duty/PagerDutyNotificationSummary';
import SlackNotificationDetails from './event-notifications/event-notification-details/SlackNotificationDetails';
import SlackNotificationForm from './event-notifications/event-notification-types/SlackNotificationForm';
import SlackNotificationSummary from './event-notifications/event-notification-types/SlackNotificationSummary';

import packageJson from '../../package.json';

const manifest = new PluginManifest(packageJson, {
  routes: [
    { path: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index, component: AWSCloudWatchApp },
  ],
  inputConfiguration: [
    {
      type: 'org.graylog.integrations.aws.inputs.AWSInput',
      component: AWSInputConfiguration,
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
});

PluginStore.register(manifest);
