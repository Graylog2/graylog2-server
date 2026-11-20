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
import { useQuery } from '@tanstack/react-query';

import { MCPOAuth } from '@graylog/server-api';

import type FetchError from 'logic/errors/FetchError';

import type { McpAuthorizationParams } from './parseAuthorizationParams';

export type McpAuthorizationRequest = {
  clientName: string;
  redirectHost: string;
  clientRegisteredAt: string;
};

const fetchAuthorizationRequest = (params: McpAuthorizationParams): Promise<McpAuthorizationRequest> =>
  MCPOAuth.authorizationRequest(
    params.client_id,
    params.redirect_uri,
    params.response_type,
    params.code_challenge,
    params.code_challenge_method,
    params.state,
    params.resource,
  ).then((details) => ({
    clientName: details.client_name,
    redirectHost: details.redirect_host,
    clientRegisteredAt: details.client_registered_at,
  }));

const useMcpAuthorizationRequest = (
  params: McpAuthorizationParams,
): {
  data: McpAuthorizationRequest | undefined;
  isLoading: boolean;
  error: FetchError | null;
} => {
  const { data, isLoading, error } = useQuery<McpAuthorizationRequest, FetchError>({
    queryKey: ['mcp-oauth', 'authorization-request', params],
    queryFn: () => fetchAuthorizationRequest(params),
    retry: false,
  });

  return { data, isLoading, error };
};

export default useMcpAuthorizationRequest;
