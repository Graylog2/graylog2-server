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
}

export = AppConfig;
