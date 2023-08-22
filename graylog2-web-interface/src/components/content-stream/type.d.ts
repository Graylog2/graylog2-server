interface ContentStreamType {
  hooks: {
    useContentStreamTag: () => string,
  },
}

declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    'content-stream'?: Array<ContentStreamType>;
  }
}
