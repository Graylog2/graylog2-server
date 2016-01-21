import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import PipelinesPage from './PipelinesPage';

PluginStore.register(new PluginManifest(packageJson, {
  routes: [
    {path: '/system/pipelines', component: PipelinesPage},
  ],

  systemnavigation: [
    {path: '/system/pipelines', description: 'Pipelines'},
  ],
}));
