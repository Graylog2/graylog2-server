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

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import type { ProfileOptions } from 'components/indices/IndexSetFieldTypeProfiles/types';
import UserNotification from 'util/UserNotification';

const INITIAL_DATA = [];

const fetchProfileOptions = async () => {
  const url = qualifyUrl('/system/indices/index_sets/profiles/all');

  return fetch('GET', url).then((profiles: Array<{name: string, id: string }>) => profiles
    .map(({ name, id }) => ({ value: id, label: name })));
};

const useProfileOptions = (): {
  options: ProfileOptions,
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['indexSetFieldTypeProfileOptions'],
    () => fetchProfileOptions(),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field type profile options failed with status: ${errorThrown}`,
          'Could not load index field type profile options');
      },
      keepPreviousData: true,
    },
  );

  return ({
    options: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  });
};

export default useProfileOptions;
