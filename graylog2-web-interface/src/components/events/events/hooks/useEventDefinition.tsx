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
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import { defaultOnError } from 'util/conditional/onError';

export const fetchEventDefinitionDetails = async (eventDefinitionId: string): Promise<EventDefinition> => (
  fetch('GET', qualifyUrl(`/events/definitions/${eventDefinitionId}`))
);

const useEventDefinition = (eventDefId: string, enabled = true) => {
  const { data, isFetching, isInitialLoading } = useQuery({
    queryKey: ['get-event-definition-details', eventDefId],
    queryFn: () => defaultOnError(fetchEventDefinitionDetails(eventDefId), 'Loading archives failed with status'),
    retry: 0,
    keepPreviousData: true,
    enabled: !!eventDefId && enabled,
  });

  return { data, isFetching, isInitialLoading };
};

export default useEventDefinition;
