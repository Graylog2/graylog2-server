import 'webpack-entry';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import Routes from 'common/Routes.js';

import AWSServices from './aws/Services';
import InputConfiguration from './aws/InputConfiguration';
import AWSCloudWatchApp from './aws-cloudwatch/CloudWatchApp';
import packageJson from '../../package.json';

const manifest = new PluginManifest(packageJson, {
  routes: [
    { path: Routes.INTEGRATIONS.AWS.SERVICES, component: AWSServices },
    { path: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index, component: AWSCloudWatchApp },
    { path: Routes.INTEGRATIONS.AWS.CLOUDWATCH.index, component: AWSCloudWatchApp },
  ],
  inputConfiguration: [
    {
      type: 'org.graylog.integrations.aws.inputs.AWSInput',
      component: InputConfiguration,
    },
  ],
});

PluginStore.register(manifest);
