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
import type { Event } from 'components/events/events/types';
import type FetchError from 'logic/errors/FetchError';
import { onError } from 'util/conditional/onError';

export const eventsUrl = (id) => qualifyUrl(`/events/${id}`);

const fetchEvent = (eventId: string) => fetch('GET', eventsUrl(eventId)).then((data) => data.event);

const useEventById = (eventId: string, { onErrorHandler }: { onErrorHandler?: (e: FetchError)=>void} = {}): {
  data: Event,
  refetch: () => void,
  isLoading: boolean,
  isFetched: boolean,
} => {
  const { data, refetch, isLoading, isFetched } = useQuery<Event>(
    ['event-by-id', eventId],
    () => onError(fetchEvent(eventId), (errorThrown: FetchError) => {
      if (onErrorHandler) onErrorHandler(errorThrown);

      UserNotification.error(`Loading event or alert failed with status: ${errorThrown}`,
        'Could not load event or alert');
    }),
    {
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
