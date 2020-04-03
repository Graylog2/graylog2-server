import { PluginStore } from 'graylog-web-plugin/plugin';

const searchTypesKey = 'searchTypes';

export default function searchTypeDefinition(type) {
  return PluginStore.exports(searchTypesKey)
    .find((s) => s.type.toLocaleUpperCase() === type.toLocaleUpperCase());
}
