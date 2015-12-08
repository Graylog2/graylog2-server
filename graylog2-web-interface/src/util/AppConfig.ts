class AppConfig {
  private appConfig: any;

  constructor() {
    this.appConfig = window['appConfig'];
  }

  gl2ServerUrl() : string {
    return this.appConfig['gl2ServerUrl'];
  }

  gl2AppPathPrefix() : string {
    return this.appConfig['gl2AppPathPrefix'];
  }

  rootTimeZone(): string {
    return this.appConfig['rootTimeZone'];
  }
}

export = AppConfig;
