import { $PropertyType } from 'utility-types/dist/utility-types';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

export interface OktaBackendConfig {
  type: 'okta';
  oktaBaseUrl: string;
  clientId: string;
  clientSecret: string;
  tokenVerifierConnectTimeout: string;
  callbackUrl: string;
}

export interface OktaBackendConfigJson {
  type: 'okta';
  okta_base_url: string;
  client_id: string;
  client_secret: string;
  token_verifier_connect_timeout: string;
  callback_url: string;
}

export interface OktaBackend {
  id: $PropertyType<AuthenticationBackend, 'id'>;
  defaultRoles: $PropertyType<AuthenticationBackend, 'defaultRoles'>;
  title: $PropertyType<AuthenticationBackend, 'title'>;
  description: $PropertyType<AuthenticationBackend, 'description'>;
  config: Omit<OktaBackendConfig, 'clientSecret'> & {
    clientSecret: { is_set: boolean };
  };
}
