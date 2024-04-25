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

import fetch from 'logic/rest/FetchProvider';
import type { CompatibilityResponseType } from 'components/datanode/Types';
import { qualifyUrl } from 'util/URLUtils';

type Options = {
  enabled: boolean,
}

const fetchCompatibility = async () => {
  const url = 'datanodes/all/rest/indices-directory/compatibility';

  return fetch('GET', qualifyUrl(url));
};

const useCompatibilityCheck = ({ enabled }: Options = { enabled: true }): {
  data: { [hostname: string]: CompatibilityResponseType },
  error: Error,
  refetch: () => void,
  isInitialLoading: boolean,
  isError: boolean,
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery<{ [hostname: string]: CompatibilityResponseType }, Error>(
    ['datanodes', 'compatibility'],
    () => fetchCompatibility(),
    {
      keepPreviousData: true,
      enabled,
    });

  return ({
    data,
    error,
    refetch,
    isInitialLoading,
    isError,
  });
};

export default useCompatibilityCheck;
