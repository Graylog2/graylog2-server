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
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

type VersionCheckType = {
  satisfied: boolean;
  errorMessage?: string;
}

export const fetchSearchVersionCheck = async ({ queryKey }) => {
  const [/* queryName */, { distribution, version }] = queryKey;

  try {
    const data = await fetch('GET', qualifyUrl(ApiRoutes.SystemSearchVersionApiController.satisfiesVersion(distribution, version).url));

    return data;
  } catch (e) {
    return UserNotification.error('Could not fetch override data.');
  }
};

const useSearchVersionCheck = (distribution: 'opensearch' | 'elasticsearch', version?: string) => {
  const MAIN_KEY = 'SearchVersionQuery';
  const queryKey = version ? [MAIN_KEY, { distribution, version }] : [MAIN_KEY, { distribution, version: null }];
  const { data, isLoading, error } = useQuery<VersionCheckType, Error>(queryKey, fetchSearchVersionCheck);

  return {
    data,
    isLoading,
    error,
  };
};

export default useSearchVersionCheck;
