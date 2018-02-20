// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import EnterprisePage from './EnterprisePage';

PluginStore.register(new PluginManifest(packageJson, {
  routes: [
    { path: '/enterprise', component: EnterprisePage },
  ],

  systemnavigation: [
    { path: '/enterprise', description: 'Enterprise' },
  ]
}));
