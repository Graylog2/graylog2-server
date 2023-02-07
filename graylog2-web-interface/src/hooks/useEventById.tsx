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

const eventsUrl = qualifyUrl('/events/search');

export type EventType = {
  alert : boolean;
  event_definition_id: string;
  event_definition_type: string;
  fields: {};
  group_by_fields: {};
  id: string;
  key: null;
  key_tuple: []
  message: string;
  origin_context: string;
  priority: number;
  replay_info:
    {
      timerange_start: string,
      timerange_end: string,
      query: string,
      streams: Array<string>
  };
  source: string;
  source_streams: Array<string>;
  streams: Array<string>;
  timerange_end: string | null;
  timerange_start: string | null;
  timestamp: string;
  timestamp_processing: string;
}

const fetchEvent = (eventId: string) => {
  return fetch('POST', eventsUrl, {
    query: `id:${eventId}`,
    timerange: { type: 'relative', range: 0 },
  }).then((data) => {
    return data.events[0].event;
  });
};

const useEventById = (eventId: string): {
  data: EventType
  refetch: () => void,
  isLoading: boolean,
  isFetched: boolean,
} => {
  const { data, refetch, isLoading, isFetched } = useQuery<EventType>(
    ['event-by-id', eventId],
    () => fetchEvent(eventId),
    {
      onError: (errorThrown) => {
        UserNotification.error(`Loading event or alert  failed with status: ${errorThrown}`,
          'Could not load event or alert');
      },
      keepPreviousData: true,
      enabled: !!eventId,
    },
  );

  return ({
    data,
    refetch,
    isLoading,
    isFetched,
  });
};

export default useEventById;
