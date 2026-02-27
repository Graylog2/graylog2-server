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

import { Users } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

const BASIC_USER_QUERY_KEY = 'basic_user';

export type BasicUser = {
  serviceAccount: boolean;
  fullName: string;
  readOnly: boolean;
  id: string;
  username: string;
};

export const getBasicUserQueryKey = (id: BasicUser['id']) => [BASIC_USER_QUERY_KEY, id];

const getUser = async ({ id }: { id: BasicUser['id'] }): Promise<BasicUser> =>
  Users.getBasicUserById(id).then((userResponse) => ({
    serviceAccount: userResponse.service_account,
    fullName: userResponse.full_name,
    readOnly: userResponse.read_only,
    id: userResponse.id,
    username: userResponse.username,
  }));

const useBasicUser = (
  userId: BasicUser['id'],
  { enabled } = { enabled: true },
): {
  data: BasicUser;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: getBasicUserQueryKey(userId),
    queryFn: () =>
      defaultOnError(
        getUser({ id: userId }),
        `Loading User ${userId} failed with status`,
        `Could not load User ${userId}`,
      ),
    enabled,
  });

  return {
    data,
    isLoading,
    refetch,
  };
};

export default useBasicUser;
