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
import URI from 'urijs';
import cloneDeep from 'lodash/cloneDeep';
import concat from 'lodash/concat';
import pick from 'lodash/pick';
import omit from 'lodash/omit';

import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { defaultOnError } from 'util/conditional/onError';
import FiltersForQueryParams from 'components/common/EntityFilters/FiltersForQueryParams';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import EventDefinitionTagsFilter from 'components/event-definitions/EventDefinitionTagsFilter';
import EventDefinitionTypeFilter from 'components/event-definitions/EventDefinitionTypeFilter';
import type { Attribute, SearchParams } from 'stores/PaginationTypes';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

type Options = {
  enabled: boolean;
};

export const EVENT_DEFINITIONS_QUERY_KEY = ['event-definitions'] as const;

const sourceUrl = '/events/definitions';

export type EventDefinitionSearchResponse = {
  elements: Array<EventDefinition>;
  query: string;
  attributes: Array<Attribute>;
  pagination: {
    count: number;
    total: number;
    page: number;
    per_page: number;
  };
};

export type EventDefinitionListResponse = {
  event_definitions: Array<EventDefinition>;
  context: Record<string, unknown>;
  query?: string;
  count?: number;
  page?: number;
  per_page?: number;
  total?: number;
  grand_total?: number;
};

export type EventDefinitionContext = {
  scheduler: {
    is_scheduled: boolean;
  };
} & Record<string, unknown>;

export type EventDefinitionGetResponse = {
  event_definition: EventDefinition;
  context: EventDefinitionContext;
  is_mutable: boolean;
};

export type EventDefinitionCopyResponse = {
  id: string;
  title: string;
};

type SubmitError = { status?: number; additional?: { body?: { failed?: boolean } } };

const eventDefinitionsUrl = ({
  segments = [],
  query = {},
}: {
  segments?: Array<string>;
  query?: Record<string, unknown>;
}) => {
  const uri = new URI(sourceUrl);
  const nextSegments = concat(uri.segment(), segments);

  uri.segmentCoded(nextSegments);
  uri.query(query);

  return qualifyUrl(uri.resource());
};

const setAlertFlag = (eventDefinition: EventDefinition) => {
  const isAlert = (eventDefinition.notifications ?? []).length > 0;

  return { ...eventDefinition, alert: isAlert };
};

const extractSchedulerInfo = (eventDefinition: EventDefinition) => {
  // Removes the internal "_is_scheduled" field from the event definition data. We only use this to pass-through
  // the flag from the form.
  const clonedEventDefinition = cloneDeep(eventDefinition);
  const { _is_scheduled } = pick(clonedEventDefinition.config, ['_is_scheduled']);

  clonedEventDefinition.config = omit(clonedEventDefinition.config, ['_is_scheduled']);

  return { eventDefinition: clonedEventDefinition, isScheduled: _is_scheduled ?? true };
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

type EventDefinitionResult = {
  list: Array<EventDefinition>;
  pagination: { total: number };
  attributes: Array<Attribute>;
};

export const fetchEventDefinitions = (searchParams: SearchParams): Promise<EventDefinitionResult> => {
  CurrentUserStore.update(CurrentUserStore.getInitialState().currentUser.username);

  const url = PaginationURL(`${sourceUrl}/paginated`, searchParams.page, searchParams.pageSize, searchParams.query, {
    sort: searchParams?.sort.attributeId,
    order: searchParams?.sort.direction,
    filters: FiltersForQueryParams(searchParams.filters),
  });

  return fetch<EventDefinitionSearchResponse>('GET', qualifyUrl(url)).then((response) => {
    const { elements, query, attributes, pagination } = response;
    const { count, total, page, per_page: perPage } = pagination;

    return {
      list: elements,
      pagination: {
        count,
        total,
        page,
        perPage,
        query,
      },
      // The backend marks `type` (config.type) filterable; attach the custom filter_component
      // here so the dropdown lists plugin-provided type display names instead of a text input.
      attributes: [
        ...(attributes ?? []).map((attribute) =>
          attribute.id === 'type' ? { ...attribute, filter_component: EventDefinitionTypeFilter } : attribute,
        ),
        TAGS_ATTRIBUTE,
      ],
    };
  });
};

// Port of the old EventDefinitionsStore `listAll` action: fetches all event definitions
// (unpaginated) together with their scheduler context. Does not notify on failure,
// matching the old store's behavior.
export const fetchAllEventDefinitions = (): Promise<EventDefinitionListResponse> =>
  fetch<EventDefinitionListResponse>('GET', eventDefinitionsUrl({ query: { per_page: 0 } }));

export const getEventDefinition = (
  eventDefinitionId: string,
): Promise<{ eventDefinition: EventDefinition; context: EventDefinitionContext; is_mutable: boolean }> =>
  fetch<EventDefinitionGetResponse>('GET', eventDefinitionsUrl({ segments: [eventDefinitionId, 'with-context'] })).then(
    (response) => ({
      eventDefinition: response.event_definition,
      context: response.context,
      is_mutable: response.is_mutable,
    }),
    (error: { status?: number }) => {
      if (error.status === 404) {
        UserNotification.error(
          `Unable to find Event Definition with id <${eventDefinitionId}>, please ensure it wasn't deleted.`,
          'Could not retrieve Event Definition',
        );
      }

      throw error;
    },
  );

export const createEventDefinition = (newEventDefinition: EventDefinition): Promise<unknown> => {
  const { eventDefinition, isScheduled } = extractSchedulerInfo(newEventDefinition);

  return fetch('POST', eventDefinitionsUrl({ query: { schedule: isScheduled } }), setAlertFlag(eventDefinition)).then(
    (response: unknown) => {
      UserNotification.success(
        'Event Definition created successfully',
        `Event Definition "${eventDefinition.title}" was created successfully.`,
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Creating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not save Event Definition',
        );
      }

      throw error;
    },
  );
};

export const copyEventDefinition = (eventDefinition: EventDefinition): Promise<EventDefinitionCopyResponse> =>
  fetch<EventDefinitionCopyResponse>('POST', eventDefinitionsUrl({ segments: [eventDefinition.id, 'duplicate'] })).then(
    (response) => {
      UserNotification.success(
        'Event Definition duplicated successfully',
        `Event Definition "${response.title}" was created successfully.`,
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Duplicating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not duplicate Event Definition',
        );
      }

      throw error;
    },
  );

export const updateEventDefinition = (
  eventDefinitionId: string,
  updatedEventDefinition: EventDefinition,
): Promise<unknown> => {
  const { eventDefinition, isScheduled } = extractSchedulerInfo(updatedEventDefinition);

  return fetch(
    'PUT',
    eventDefinitionsUrl({ segments: [eventDefinitionId], query: { schedule: isScheduled } }),
    setAlertFlag(eventDefinition),
  ).then(
    (response: unknown) => {
      UserNotification.success(
        'Event Definition updated successfully',
        `Event Definition "${eventDefinition.title}" was updated successfully.`,
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Updating Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not update Event Definition',
        );
      }

      throw error;
    },
  );
};

export const deleteEventDefinition = (eventDefinition: EventDefinition): Promise<unknown> =>
  fetch('DELETE', eventDefinitionsUrl({ segments: [eventDefinition.id] }));

export const enableEventDefinition = (eventDefinition: EventDefinition): Promise<unknown> =>
  fetch('PUT', eventDefinitionsUrl({ segments: [eventDefinition.id, 'schedule'] })).then(
    (response: unknown) => {
      UserNotification.success(
        'Event Definition successfully enabled',
        `Event Definition "${eventDefinition.title}" was successfully enabled.`,
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Enabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not enable Event Definition',
        );
      }

      throw error;
    },
  );

export const disableEventDefinition = (eventDefinition: EventDefinition): Promise<unknown> =>
  fetch('PUT', eventDefinitionsUrl({ segments: [eventDefinition.id, 'unschedule'] })).then(
    (response: unknown) => {
      UserNotification.success(
        'Event Definition successfully disabled',
        `Event Definition "${eventDefinition.title}" was successfully disabled.`,
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Disabling Event Definition "${eventDefinition.title}" failed with status: ${error}`,
          'Could not disable Event Definition',
        );
      }

      throw error;
    },
  );

export const clearNotificationQueue = (eventDefinition: EventDefinition): Promise<unknown> =>
  fetch('PUT', eventDefinitionsUrl({ segments: [eventDefinition.id, 'clear-notification-queue'] })).then(
    (response: unknown) => {
      UserNotification.success('Queued notifications cleared.', 'Queued notifications were successfully cleared.');

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          `Clearing queued notifications failed with status: ${error}`,
          'Could not clear queued notifications',
        );
      }

      throw error;
    },
  );

// Query key for the event definitions overview table (PaginatedEntityTable), which supplies
// its own (notifying) queryFn. Kept distinct from the `useEventDefinitions` hook key below
// to avoid sharing a query key between different queryFns.
export const keyFn = (searchParams: SearchParams) => [
  ...EVENT_DEFINITIONS_QUERY_KEY,
  'overview-paginated',
  searchParams,
];

export const useEventDefinitionWithContext = (eventDefinitionId: string) => {
  const { data, isFetching } = useQuery({
    queryKey: [...EVENT_DEFINITIONS_QUERY_KEY, eventDefinitionId],

    queryFn: () =>
      defaultOnError(
        getEventDefinition(eventDefinitionId),
        'Loading Event Definition failed with status',
        'Could not load Event definition',
      ),
    retry: false,
  });

  return {
    data: isFetching ? null : data,
    isFetching,
  };
};

// Replacement for the old `EventDefinitionsActions.listAll` / `eventDefinitions.all` store state:
// consumers can read `{ all: data?.event_definitions, context: data?.context }`.
export const useAllEventDefinitions = () =>
  useQuery({
    queryKey: [...EVENT_DEFINITIONS_QUERY_KEY, 'all'],
    queryFn: fetchAllEventDefinitions,
  });

const useEventDefinitions = (searchParams: SearchParams, { enabled }: Options = { enabled: true }) => {
  const { data, refetch, isInitialLoading } = useQuery({
    queryKey: [...EVENT_DEFINITIONS_QUERY_KEY, 'paginated', searchParams],

    queryFn: () =>
      defaultOnError(
        fetchEventDefinitions(searchParams),
        'Loading Event Definitions failed with status',
        'Could not load Event definition',
      ),
    placeholderData: keepPreviousData,
    retry: false,
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
    // Deliberately not under EVENT_DEFINITIONS_QUERY_KEY: this is not event definition
    // entity data, so it should not be refetched on event definition mutations.
    queryKey: ['event-definition-entity-types'],
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
    // Deliberately not under EVENT_DEFINITIONS_QUERY_KEY: this is cluster configuration,
    // not event definition entity data, so it should not be refetched on event definition mutations.
    queryKey: ['events-cluster-config'],
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
