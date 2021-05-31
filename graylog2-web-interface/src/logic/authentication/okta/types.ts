/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import { $PropertyType } from 'utility-types/dist/utility-types';

import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

export interface OktaBackendConfig {
  type: 'okta';
  oktaBaseUrl: string;
  clientId: string;
  clientSecret: string;
  apiToken: string;
  tokenVerifierConnectTimeout: string;
  callbackUrl: string;
}

export interface OktaBackendConfigJson {
  type: 'okta';
  okta_base_url: string;
  client_id: string;
  client_secret: string;
  api_token: string;
  token_verifier_connect_timeout: string;
  callback_url: string;
}

export interface OktaBackend {
  id: $PropertyType<AuthenticationBackend, 'id'>;
  defaultRoles: $PropertyType<AuthenticationBackend, 'defaultRoles'>;
  title: $PropertyType<AuthenticationBackend, 'title'>;
  description: $PropertyType<AuthenticationBackend, 'description'>;
  config: Omit<OktaBackendConfig, 'clientSecret' | 'apiToken'> & {
    apiToken: { is_set: boolean };
    clientSecret: { is_set: boolean };
  };
}
