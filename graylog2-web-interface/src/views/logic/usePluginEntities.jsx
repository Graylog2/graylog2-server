// @flow strict
import { PluginStore } from 'graylog-web-plugin/plugin';

const usePluginEntities = <T>(entityKey: string): Array<T> => PluginStore.exports(entityKey);

export default usePluginEntities;
