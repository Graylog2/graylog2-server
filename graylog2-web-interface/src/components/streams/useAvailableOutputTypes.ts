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

import type { ConfigurationField } from 'components/configurationforms';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

export const KEY_PREFIX = ['outputs', 'types'];
export const keyFn = () => [...KEY_PREFIX];

export type AvailableOutputRequestedConfiguration = {
  [key: string]: ConfigurationField,
};
export type AvailableOutputSummary = {
  human_name: string;
  requested_configuration: AvailableOutputRequestedConfiguration,
  link_to_docs: string;
  name: string;
  type: string;
};
export type AvailableOutputTypes = {
  [_key: string]: {
    [_key: string]: AvailableOutputSummary;
  };
};

export const fetchOutputsTypes = () => {
  const url = qualifyUrl(ApiRoutes.OutputsApiController.availableTypes().url);

  return fetch('GET', url);
};

type Options = {
  enabled: boolean,
}

const useAvailableOutputTypes = ({ enabled }: Options = { enabled: true }): {
  data: AvailableOutputTypes,
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery(
    keyFn(),
    () => defaultOnError(fetchOutputsTypes(), 'Loading stream outputs failed with status', 'Could not load stream outputs'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
  });
};

export default useAvailableOutputTypes;
