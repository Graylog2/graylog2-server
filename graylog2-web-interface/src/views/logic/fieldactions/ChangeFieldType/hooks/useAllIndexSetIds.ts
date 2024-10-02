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

export type SimpleIndexSet = { id: string, type: string };
type State = Array<SimpleIndexSet>;
const INITIAL_DATA: State = [];

const url = qualifyUrl('system/indices/index_sets/types/index_sets_with_field_type_change_support');

const fetchAllIndexSetIds = async (streams: Array<string>): Promise<State> => fetch<Array<{index_set_id: string, type: string }>>('POST', url, streams?.length ? streams : undefined).then(
  (elements) => {
    const list: Array<SimpleIndexSet> = elements.map((element) => ({
      id: element.index_set_id,
      type: element.type,
    }));

    return list;
  },
);

const useAllIndexSetIds = (streams: Array<string>): {
  data: State,
  isLoading: boolean,
  refetch: () => void,
} => {
  const { data, isLoading, refetch } = useQuery(
    ['allIndexSetIds', ...streams],
    () => fetchAllIndexSetIds(streams),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading index sets with field type change support failed with status: ${errorThrown}`,
          'Could not index sets with field type change support');
      },
    },
  );

  return ({
    data: data ?? INITIAL_DATA,
    isLoading,
    refetch,
  });
};

export default useAllIndexSetIds;
