const AppConfig = {
  gl2ServerUrl() {
    return window.appConfig.gl2ServerUrl;
  },

  gl2AppPathPrefix() {
    const appPrefix = window.appConfig.gl2AppPathPrefix;
    return appPrefix.endsWith('/') ? appPrefix : `${appPrefix}/`;
  },

  rootTimeZone() {
    return window.appConfig.rootTimeZone;
  },
};

export default AppConfig;
