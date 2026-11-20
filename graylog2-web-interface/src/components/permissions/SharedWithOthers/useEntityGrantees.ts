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

import { AuthorizationShares } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

import type { GranteeShare } from './shareCounts';

const useEntityGrantees = (
  grn: string,
): {
  data: Array<GranteeShare> | undefined;
  isInitialLoading: boolean;
  isError: boolean;
} => {
  const { data, isInitialLoading, isError } = useQuery({
    queryKey: ['authz', 'shares', 'grantees', grn],
    queryFn: () =>
      defaultOnError(
        AuthorizationShares.getGrantees(grn),
        'Loading sharing information failed with status',
        'Could not load sharing information',
      ).then(({ grantees }) => grantees),
    // defaultOnError already shows a notification, so a retry would show another one.
    retry: false,
  });

  return { data, isInitialLoading, isError };
};

export default useEntityGrantees;
