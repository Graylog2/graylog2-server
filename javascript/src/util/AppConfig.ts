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

  gl2UserTimeZone() : string {
    return this.appConfig['gl2UserTimeZone'];
  }

  gl2UserTimeZoneOffset() : number {
    return this.appConfig['gl2UserTimeZoneOffset'];
  }

  gl2UserSessionId() : string {
    return this.appConfig['gl2UserSessionId'];
  }

  sockJsWebSocketsEnabled() : boolean {
    return this.appConfig['sockJsWebSocketsEnabled'];
  }
}

export = AppConfig;
