export interface OktaBackendConfig {
  oktaBaseUrl: string;
  clientId: string;
  clientSecret: string;
  tokenVerifierConnectTimeout: number;
  callbackUrl: string;
}

export interface OktaBackendConfigJson {
  okta_base_url: string;
  client_id: string;
  client_secret: string;
  token_verifier_connect_timeout: number;
  callback_url: string;
}
