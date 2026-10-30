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
import qs from 'qs';

export type McpAuthorizationParams = {
  client_id: string;
  redirect_uri: string;
  response_type: string;
  code_challenge: string;
  code_challenge_method: string;
  state: string;
  resource: string;
};

const parseAuthorizationParams = (search: string): McpAuthorizationParams => {
  const params = qs.parse(search, { ignoreQueryPrefix: true });
  const get = (name: keyof McpAuthorizationParams) => {
    const value = params[name];

    return typeof value === 'string' ? value : undefined;
  };

  return {
    client_id: get('client_id'),
    redirect_uri: get('redirect_uri'),
    response_type: get('response_type'),
    code_challenge: get('code_challenge'),
    code_challenge_method: get('code_challenge_method'),
    state: get('state'),
    resource: get('resource'),
  };
};

export default parseAuthorizationParams;
