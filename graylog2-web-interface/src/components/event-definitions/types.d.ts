interface EventDefinitionType {
  type: string;
  displayName: string;
}
declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'eventDefinitionTypes'?: Array<EventDefinitionType>;
  }
}
