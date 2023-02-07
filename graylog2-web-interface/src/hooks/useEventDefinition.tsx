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

const definitionsUrl = qualifyUrl('/events/definitions');

const fetchDefinition = (definitionId: string) => {
  return fetch('GET', `${definitionsUrl}/${definitionId}`);
};

const useEventDefinition = (definitionId: string): {
  data: any
  refetch: () => void,
  isLoading: boolean,
  isFetched: boolean
} => {
  const { data, refetch, isLoading, isFetched } = useQuery(
    ['definition', definitionId],
    () => fetchDefinition(definitionId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading event definition  failed with status: ${errorThrown}`,
          'Could not load definition');
      },
      keepPreviousData: true,
      enabled: !!definitionId,
    },
  );

  return ({
    data,
    refetch,
    isLoading,
    isFetched,
  });
};

export default useEventDefinition;
