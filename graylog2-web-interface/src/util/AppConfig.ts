class AppConfig {
  private appConfig: any;

  gl2ServerUrl() : string {
    return window['appConfig']['gl2ServerUrl'];
  }

  gl2AppPathPrefix() : string {
    return window['appConfig']['gl2AppPathPrefix'];
  }

  rootTimeZone(): string {
    return window['appConfig']['rootTimeZone'];
  }
}

export = AppConfig;
