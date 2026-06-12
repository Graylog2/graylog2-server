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
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { EventDefinitionsStore } from 'stores/event-definitions/EventDefinitionsStore';
import { EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import { defaultOnError } from 'util/conditional/onError';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import EventDefinitionTagsFilter from 'components/event-definitions/EventDefinitionTagsFilter';
import EventDefinitionTypeFilter from 'components/event-definitions/EventDefinitionTypeFilter';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';

type Options = {
  enabled: boolean;
};

// The Tags column is registered as a frontend-only Attribute so we can attach a custom
// filter_component (the autocomplete-backed EventDefinitionTagsFilter). The filter dropdown
// reads only from the backend-returned attribute list, so we splice it in here.
const TAGS_ATTRIBUTE: Attribute = {
  id: 'tags',
  title: 'Tags',
  type: 'STRING',
  sortable: false,
  searchable: true,
  filterable: true,
  filter_component: EventDefinitionTagsFilter,
};

export const fetchEventDefinitions = (searchParams: SearchParams): Promise<EventDefinitionResult> => {
  CurrentUserStore.update(CurrentUserStore.getInitialState().currentUser.username);

  return EventDefinitionsStore.searchPaginated(searchParams.page, searchParams.pageSize, searchParams.query, {
    sort: searchParams?.sort.attributeId,
    order: searchParams?.sort.direction,
    filters: FiltersForQueryParams(searchParams.filters),
  }).then(({ elements, pagination, attributes }) => ({
    list: elements,
    pagination,
    // The backend marks `type` (config.type) filterable; attach the custom filter_component
    // here so the dropdown lists plugin-provided type display names instead of a text input.
    attributes: [
      ...(attributes ?? []).map((attribute) =>
        attribute.id === 'type' ? { ...attribute, filter_component: EventDefinitionTypeFilter } : attribute,
      ),
      TAGS_ATTRIBUTE,
    ],
  }));
};

export const fetchEventDefinition = (eventDefinitionId: string): Promise<any> =>
  EventDefinitionsStore.get(eventDefinitionId).then(({ event_definition, context, is_mutable }) => ({
    eventDefinition: event_definition,
    context: context,
    is_mutable: is_mutable,
  }));

export const keyFn = (searchParams: SearchParams) => ['eventDefinition', 'overview', searchParams];

type EventDefinitionResult = {
  list: Array<EventDefinition>;
  pagination: { total: number };
  attributes: Array<{ id: string; title: string; sortable: boolean }>;
};

export const useGetEventDefinition = (eventDefinitionId: string) => {
  const { data, isFetching } = useQuery({
    queryKey: ['get-event-definition', eventDefinitionId],

    queryFn: () =>
      defaultOnError(
        fetchEventDefinition(eventDefinitionId),
        'Loading Event Definition failed with status',
        'Could not load Event definition',
      ),
  });

  return {
    data: isFetching ? null : data,
    isFetching,
  };
};

const useEventDefinitions = (searchParams: SearchParams, { enabled }: Options = { enabled: true }) => {
  const { data, refetch, isInitialLoading } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError(
        fetchEventDefinitions(searchParams),
        'Loading Event Definitions failed with status',
        'Could not load Event definition',
      ),
    placeholderData: keepPreviousData,
    enabled,
  });

  return {
    data,
    refetch,
    isInitialLoading,
  };
};

export default useEventDefinitions;

export function useGetEntityTypes() {
  const { data, isLoading } = useQuery({
    queryKey: ['event-definitions', 'entity-types'],
    queryFn: () =>
      defaultOnError(
        fetch('GET', qualifyUrl('/events/entity_types')),
        'Loading event definition entity types failed with status',
        'Could not load event definition entity types',
      ),
  });

  return {
    entityTypes: isLoading ? [] : data || [],
    loadingEntityTypes: isLoading,
  };
}

export function useGetListEventsClusterConfig(): {
  eventsClusterConfig: { [key: string]: unknown };
  loadingEventsClusterConfig: boolean;
} {
  const { data, isLoading } = useQuery<{}, Error>({
    queryKey: ['event-definitions', 'list-events-cluster-config'],
    queryFn: () =>
      defaultOnError(
        ConfigurationsActions.listEventsClusterConfig(),
        'Loading event definition list events cluster config failed with status',
        'Could not load event definition list events cluster config',
      ),
  });

  return {
    eventsClusterConfig: isLoading ? {} : data || {},
    loadingEventsClusterConfig: isLoading,
  };
}

export function useGetEventNotifications(): {
  eventNotifications: { all: Array<EventNotification> };
  loadingEventNotifications: boolean;
} {
  const { data, isLoading } = useQuery<{ notifications: Array<EventNotification> }, Error>({
    queryKey: ['event-definitions', 'event-notifications'],
    queryFn: () =>
      defaultOnError(
        EventNotificationsStore.listAll(),
        'Loading event notifications failed with status',
        'Could not load event notifications',
      ),
  });

  return {
    eventNotifications: { all: isLoading ? [] : data?.notifications || [] },
    loadingEventNotifications: isLoading,
  };
}
