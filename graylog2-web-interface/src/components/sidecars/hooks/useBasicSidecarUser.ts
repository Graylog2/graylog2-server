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

import { Sidecar } from '@graylog/server-api';

import { defaultOnError } from 'util/conditional/onError';

const BASIC_FORWARDER_USER_QUERY_KEY = 'basic_forwarder_user';

export type BasicSidecarUser = {
  serviceAccount: boolean;
  fullName: string;
  readOnly: boolean;
  id: string;
  username: string;
};

const getSidecarUser = async (): Promise<BasicSidecarUser> =>
  Sidecar.getBasicSidecarUser().then((userResponse) => ({
    serviceAccount: userResponse.service_account,
    fullName: userResponse.full_name,
    readOnly: userResponse.read_only,
    id: userResponse.id,
    username: userResponse.username,
  }));

type UseBasicSidecarUserOptions = {
  enabled?: boolean;
};

const useBasicSidecarUser = ({ enabled = true }: UseBasicSidecarUserOptions = {}): {
  data: BasicSidecarUser;
  isLoading: boolean;
  refetch: () => void;
} => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: [BASIC_FORWARDER_USER_QUERY_KEY],
    queryFn: () =>
      defaultOnError(getSidecarUser(), `Loading Sidecar User failed with status`, `Could not load Sidecar User`),
    enabled,
  });

  return {
    data,
    isLoading,
    refetch,
  };
};

export default useBasicSidecarUser;
