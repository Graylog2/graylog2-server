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
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const fetchJournalDowntimeSize = async () => fetch('GET', qualifyUrl('/migration/journalestimate'));

const useJournalDowntimeSize = () : {
  data: number,
  refetch: () => void,
  isInitialLoading: boolean,
  error: any,
  isError: boolean
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery(
    ['JournalDowntimeSize'],
    fetchJournalDowntimeSize,
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading Data Node migration journal estimate: ${errorThrown}`,
          'Could not load Data Node journal size estimate');
      },
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval: 5000,
    },
  );

  return ({
    data: Math.round((data || 0) / 1024), // KB
    refetch,
    isInitialLoading,
    error,
    isError,
  });
};

export default useJournalDowntimeSize;
