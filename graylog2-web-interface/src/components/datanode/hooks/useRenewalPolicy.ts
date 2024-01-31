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

import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import ConfigurationType from 'components/configurations/ConfigurationTypes';
import type { RenewalPolicy } from 'components/datanode/Types';
import type FetchError from 'logic/errors/FetchError';
import UserNotification from 'util/UserNotification';

const queryKey = ['config', 'certificate-renewal-policy'];
const fetchCurrentConfig = () => ConfigurationsActions.list(ConfigurationType.CERTIFICATE_RENEWAL_POLICY_CONFIG) as Promise<RenewalPolicy>;

const UseRenewalPolicy = (refetchInterval: number | false = 3000): {
  data: RenewalPolicy,
  isInitialLoading: boolean,
  error: FetchError | null,
} => {
  const { data, isInitialLoading, error } = useQuery<RenewalPolicy, FetchError>(queryKey, fetchCurrentConfig, {
    initialData: undefined,
    refetchInterval,
    retry: false,
    onError: (newError) => {
      UserNotification.error(`Loading Data Nodes failed with status: ${newError}`,
        'Could not load Data Nodes');
    },
  });

  return {
    data,
    isInitialLoading,
    error,
  };
};

export default UseRenewalPolicy;
