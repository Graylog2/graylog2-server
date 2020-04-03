const AppConfig = {
  gl2ServerUrl() {
    if (typeof (GRAYLOG_HTTP_PUBLISH_URI) !== 'undefined') {
      // The GRAYLOG_HTTP_PUBLISH_URI variable will be set by webpack via the DefinePlugin.
      // eslint-disable-next-line no-undef
      return GRAYLOG_HTTP_PUBLISH_URI;
    }
    return this.appConfig().gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    return this.appConfig().gl2AppPathPrefix;
  },

  gl2DevMode() {
    // The DEVELOPMENT variable will be set by webpack via the DefinePlugin.
    // eslint-disable-next-line no-undef
    return typeof (DEVELOPMENT) !== 'undefined' && DEVELOPMENT;
  },

  isFeatureEnabled(feature) {
    // eslint-disable-next-line no-undef
    return typeof (FEATURES) !== 'undefined' && FEATURES.split(',')
      .filter((s) => typeof s === 'string')
      .map((s) => s.trim().toLowerCase())
      .includes(feature.toLowerCase());
  },

  rootTimeZone() {
    return this.appConfig().rootTimeZone;
  },

  appConfig() {
    return window.appConfig || {};
  },
};

export default AppConfig;
