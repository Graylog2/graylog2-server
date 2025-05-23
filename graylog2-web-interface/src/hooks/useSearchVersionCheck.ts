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
import ApiRoutes from 'routing/ApiRoutes';
import { defaultOnError } from 'util/conditional/onError';

type VersionCheckType = {
  satisfied: boolean;
  errorMessage?: string;
};

export const fetchSearchVersionCheck = ({ queryKey }): Promise<VersionCheckType> => {
  const [, /* queryName */ { distribution, version }] = queryKey;

  return fetch(
    'GET',
    qualifyUrl(ApiRoutes.SystemSearchVersionApiController.satisfiesVersion(distribution, version).url),
  );
};

const useSearchVersionCheck = (distribution: 'opensearch' | 'elasticsearch' | 'datanode', version?: string) => {
  const MAIN_KEY = 'SearchVersionQuery';
  const queryKey = [MAIN_KEY, { distribution, version: version ?? null }];
  const { data, isLoading, error } = useQuery({
    queryKey,
    queryFn: (args) => defaultOnError(fetchSearchVersionCheck(args), 'Could not fetch override data'),
  });

  return {
    data,
    isLoading,
    error,
  };
};

export default useSearchVersionCheck;
