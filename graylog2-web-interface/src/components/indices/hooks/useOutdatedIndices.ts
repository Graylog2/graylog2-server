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
import { qualifyUrl } from 'util/URLUtils';
import { defaultOnError } from 'util/conditional/onError';

export type OutdatedIndex = {
  index_name: string;
  version: string;
  warm_index: boolean;
  managed_index: boolean;
  system_index: boolean;
};

const OUTDATED_INDICES_URL = qualifyUrl('/system/indexer/indices/outdated');

const fetchOutdatedIndices = (): Promise<Array<OutdatedIndex>> => fetch('GET', OUTDATED_INDICES_URL);

const useOutdatedIndices = () => {
  const {
    data = [],
    isError,
    isLoading,
  } = useQuery({
    queryKey: ['outdatedIndices'],
    queryFn: () =>
      defaultOnError(fetchOutdatedIndices(), 'Loading outdated indices failed', 'Could not load outdated indices'),
    retry: false,
  });

  return {
    data,
    isError,
    isLoading,
  };
};

export default useOutdatedIndices;
