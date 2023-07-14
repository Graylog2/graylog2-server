import type { PluginRegistration } from 'graylog-web-plugin/plugin';
import { PluginStore } from 'graylog-web-plugin/plugin';

export const loadPlugin = (plugin: PluginRegistration) => PluginStore.register(plugin);
export const unloadPlugin = (plugin: PluginRegistration) => PluginStore.unregister(plugin);

export const usePlugin = (plugin: PluginRegistration) => {
  beforeAll(() => loadPlugin(plugin));
  afterAll(() => unloadPlugin(plugin));
};
