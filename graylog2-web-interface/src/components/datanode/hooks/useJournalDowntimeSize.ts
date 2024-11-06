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
import moment from 'moment';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { defaultOnError } from 'util/conditional/onError';

const fetchJournalDowntimeSize = async () => fetch('GET', qualifyUrl('/migration/journalestimate'));

const useJournalDowntimeSize = () : {
  data: {
    KBs_per_minute: number,
    journal_size_MB: number,
    max_downtime_duration: string,
  },
  refetch: () => void,
  isInitialLoading: boolean,
  error: any,
  isError: boolean
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery(
    ['JournalDowntimeSize'],
    () => defaultOnError(fetchJournalDowntimeSize(), 'Loading Data Node migration journal estimate', 'Could not load Data Node journal size estimate'),
    {
      notifyOnChangeProps: ['data', 'error'],
      refetchInterval: 5000,
    },
  );

  return ({
    data: {
      KBs_per_minute: Math.ceil((data?.bytes_per_minute || 0) / 1024), // KB/min
      journal_size_MB: Math.ceil((data?.journal_size || 0) / (1024 * 1024)), // MB
      max_downtime_duration: moment.duration(data?.max_downtime_minutes || 0, 'minutes').format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all', useToLocaleString: false }),
    },
    refetch,
    isInitialLoading,
    error,
    isError,
  });
};

export default useJournalDowntimeSize;
