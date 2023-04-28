interface LoggerPlugin {
  EnterpriseSupportBundleInfo: React.ComponentType<{}>;
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    logger?: LoggerPlugin;
  }
}
