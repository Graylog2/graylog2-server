// eslint-disable-next-line no-unused-vars
import webpackEntry from 'webpack-entry';

import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import PipelinesOverviewPage from 'pipelines/PipelinesOverviewPage';
import PipelineDetailsPage from 'pipelines/PipelineDetailsPage';
import SimulatorPage from 'simulator/SimulatorPage';
import RulesPage from 'rules/RulesPage';
import RuleDetailsPage from 'rules/RuleDetailsPage';

PluginStore.register(new PluginManifest(packageJson, {
  routes: [
    { path: '/system/pipelines', component: PipelinesOverviewPage },
    { path: '/system/pipelines/rules', component: RulesPage },
    { path: '/system/pipelines/rules/:ruleId', component: RuleDetailsPage },
    { path: '/system/pipelines/simulate', component: SimulatorPage },
    { path: '/system/pipelines/:pipelineId', component: PipelineDetailsPage },
  ],

  systemnavigation: [
    { path: '/system/pipelines', description: 'Pipelines', permissions: 'inputs:create' },
  ],
}));
