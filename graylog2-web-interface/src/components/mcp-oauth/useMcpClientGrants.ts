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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { MCPOAuth } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';
import UserNotification from 'util/UserNotification';

export type McpClientGrant = {
  clientId: string;
  clientName: string;
  lastRefreshedAt: string;
  expiresAt: string;
};

const GRANTS_QUERY_KEY = ['mcp-oauth', 'grants'];

const fetchGrants = (): Promise<Array<McpClientGrant>> =>
  MCPOAuth.grants().then(({ grants }) =>
    grants.map((grant) => ({
      clientId: grant.client_id,
      clientName: grant.client_name,
      lastRefreshedAt: grant.last_refreshed_at,
      expiresAt: grant.expires_at,
    })),
  );

const useMcpClientGrants = (
  { enabled } = { enabled: true },
): {
  grants: Array<McpClientGrant>;
  isLoading: boolean;
  revokeGrant: (clientId: string) => Promise<void>;
  isRevoking: boolean;
} => {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: GRANTS_QUERY_KEY,
    queryFn: () => defaultOnError(fetchGrants(), 'Loading connected MCP clients failed with status'),
    enabled,
  });
  const { mutateAsync, isPending } = useMutation({
    mutationFn: (clientId: string) => MCPOAuth.revokeGrant(clientId),
    onSuccess: () => {
      UserNotification.success('Access of the MCP client has been revoked.');

      return queryClient.invalidateQueries({ queryKey: GRANTS_QUERY_KEY });
    },
    onError: (error) => UserNotification.error(`Revoking access failed: ${error}`, 'Could not revoke access'),
  });

  return { grants: data ?? [], isLoading, revokeGrant: mutateAsync, isRevoking: isPending };
};

export default useMcpClientGrants;
