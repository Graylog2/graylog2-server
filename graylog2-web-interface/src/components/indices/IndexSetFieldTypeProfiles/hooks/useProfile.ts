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

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type {
  IndexSetFieldTypeProfileJson,
  IndexSetFieldTypeProfile,
} from 'components/indices/IndexSetFieldTypeProfiles/types';

const INITIAL_DATA: IndexSetFieldTypeProfile = {
  customFieldMappings: [],
  name: null,
  id: null,
  description: null,
  indexSetIds: [],
};

const fetchIndexSetFieldTypeProfile = async (id: string) => {
  const url = qualifyUrl(`/system/indices/index_sets/profiles/${id}`);

  return fetch('GET', url).then((profile: IndexSetFieldTypeProfileJson) => ({
    id: profile.id,
    name: profile.name,
    description: profile.description,
    customFieldMappings: profile.custom_field_mappings,
    indexSetIds: profile.index_set_ids,
  }));
};

const useProfile = (id: string): {
  data: IndexSetFieldTypeProfile,
  isFetched: boolean,
  isFetching: boolean,
  refetch: () => void,
} => {
  const { data, isFetched, isFetching, refetch } = useQuery(
    ['indexSetFieldTypeProfile', id],
    () => fetchIndexSetFieldTypeProfile(id),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index field type profile failed with status: ${errorThrown}`,
          'Could not load index field type profile');
      },
      keepPreviousData: true,
      enabled: !!id,
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isFetched,
    isFetching,
    refetch,
  });
};

export default useProfile;
