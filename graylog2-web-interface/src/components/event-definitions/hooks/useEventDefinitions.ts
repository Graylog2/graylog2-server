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

import type { SearchParams } from 'stores/PaginationTypes';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import { EventDefinitionsStore } from 'stores/event-definitions/EventDefinitionsStore';
import { defaultOnError } from 'util/conditional/onError';

type Options = {
  enabled: boolean,
}

export const fetchEventDefinitions = (searchParams: SearchParams) => EventDefinitionsStore.searchPaginated(
  searchParams.page,
  searchParams.pageSize,
  searchParams.query,
  { sort: searchParams?.sort.attributeId, order: searchParams?.sort.direction },
).then(({ elements, pagination, attributes }) => ({
  list: elements,
  pagination,
  attributes,
}));

export const keyFn = (searchParams: SearchParams) => ['eventDefinition', 'overview', searchParams];

type EventDefinitionResult = {
  list: Array<EventDefinition>,
  pagination: { total: number }
  attributes: Array<{ id: string, title: string, sortable: boolean }>
};

const useEventDefinitions = (searchParams: SearchParams, { enabled }: Options = { enabled: true }): {
  data: EventDefinitionResult | undefined,
  refetch: () => void,
  isInitialLoading: boolean,
} => {
  const { data, refetch, isInitialLoading } = useQuery<EventDefinitionResult>(
    keyFn(searchParams),
    () => defaultOnError(fetchEventDefinitions(searchParams), 'Loading Event Definitions failed with status', 'Could not load Event definition'),
    {
      keepPreviousData: true,
      enabled,
    },
  );

  return ({
    data,
    refetch,
    isInitialLoading,
  });
};

export default useEventDefinitions;
