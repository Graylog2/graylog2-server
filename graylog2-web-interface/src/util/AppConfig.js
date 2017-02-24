const AppConfig = {
  gl2ServerUrl() {
    return window.appConfig.gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    return window.appConfig.gl2AppPathPrefix;
  },

  gl2DevMode() {
    // The DEVELOPMENT variable will be set by webpack via the DefinePlugin.
    // eslint-disable-next-line no-undef
    return typeof (DEVELOPMENT) !== 'undefined' && DEVELOPMENT;
  },

  rootTimeZone() {
    return window.appConfig.rootTimeZone;
  },
};

export default AppConfig;
