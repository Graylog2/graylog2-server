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
import { useMutation } from '@tanstack/react-query';

import { MCPOAuth } from '@graylog/server-api';

import type FetchError from 'logic/errors/FetchError';

import type { McpAuthorizationParams } from './parseAuthorizationParams';

const useMcpAuthorizationDecision = (
  params: McpAuthorizationParams,
): {
  decide: (approved: boolean) => Promise<string>;
  isPending: boolean;
  error: FetchError | null;
} => {
  const { mutateAsync, isPending, error } = useMutation<string, FetchError, boolean>({
    mutationFn: (approved: boolean) =>
      MCPOAuth.decide({ ...params, approved }).then(({ redirect_uri }) => redirect_uri),
  });

  return { decide: mutateAsync, isPending, error };
};

export default useMcpAuthorizationDecision;
