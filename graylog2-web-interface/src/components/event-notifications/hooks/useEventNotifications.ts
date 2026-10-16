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
import concat from 'lodash/concat';

import { EventsNotifications } from '@graylog/server-api';

import i18n from 'i18n';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import CancellablePromise from 'logic/rest/CancellablePromise';
import { defaultOnError } from 'util/conditional/onError';
import type { SearchParams } from 'stores/PaginationTypes';
import type { EntityShare } from 'actions/permissions/EntityShareActions';

export type TestResult = {
  isLoading: boolean;
  id?: string;
  error?: boolean;
  message?: string;
};

export type TestResults = {
  [key: string]: TestResult;
};

export type EventNotification = {
  id: string;
  title: string;
  description: string;
  config: { type: string; [key: string]: any };
};

export type LegacyEventNotification = {
  name: string;
  configuration: { [key: string]: { human_name: string } };
};

export const EVENT_NOTIFICATIONS_QUERY_KEY = ['event-notifications'] as const;

const sourceUrl = '/events/notifications';

const eventNotificationsUrl = ({
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

type SubmitError = { status?: number; additional?: { body?: { failed?: boolean } } };

type EventNotificationsResult = {
  list: Array<EventNotification>;
  pagination: { total: number };
  attributes: Array<{ id: string; title: string; sortable: boolean }>;
};

// Port of the old EventNotificationsStore `searchPaginated` action.
export const fetchEventNotifications = (searchParams: SearchParams): Promise<EventNotificationsResult> =>
  EventsNotifications.getPage(
    searchParams.sort.attributeId as Parameters<typeof EventsNotifications.getPage>[0],
    searchParams.page,
    searchParams.pageSize,
    searchParams.query || undefined,
    searchParams.sort.direction,
  ).then(({ elements, query, attributes, pagination }) => ({
    list: elements,
    pagination: {
      count: pagination.count,
      total: pagination.total,
      page: pagination.page,
      perPage: pagination.per_page,
      query,
    },
    attributes,
  }));

// Port of the old EventNotificationsStore `listAll` action: fetches all event notifications (unpaginated).
// Does not notify on failure, matching the old store's behavior.
export const fetchAllEventNotifications = (): Promise<{ notifications: Array<EventNotification> }> =>
  fetch('GET', eventNotificationsUrl({ query: { per_page: 0 } }));

// Port of the old EventNotificationsStore `listAllLegacyTypes` action.
export const fetchLegacyEventNotificationTypes = (): Promise<{
  types: { [key: string]: LegacyEventNotification };
}> => fetch('GET', eventNotificationsUrl({ segments: ['legacy', 'types'] }));

export const getEventNotification = (notificationId: string): Promise<EventNotification> =>
  EventsNotifications.get(encodeURIComponent(notificationId)).then(
    (response) => response,
    (error: { status?: number }) => {
      if (error.status === 404) {
        UserNotification.error(
          i18n.t('messages.not_found', { ns: 'event_notifications', id: notificationId }),
          i18n.t('messages.not_found_title', { ns: 'event_notifications' }),
        );
      }

      throw error;
    },
  );

export const createEventNotification = (notification: EventNotification & EntityShare): Promise<unknown> => {
  const { share_request, ...rest } = notification;

  return fetch('POST', eventNotificationsUrl({}), { entity: rest, share_request }).then(
    (response: unknown) => {
      UserNotification.success(
        i18n.t('messages.created', { ns: 'event_notifications' }),
        i18n.t('messages.created_title', { ns: 'event_notifications', title: notification.title }),
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          i18n.t('messages.create_failed', {
            ns: 'event_notifications',
            title: notification.title,
            error: String(error),
          }),
          i18n.t('messages.create_failed_title', { ns: 'event_notifications' }),
        );
      }

      throw error;
    },
  );
};

export const updateEventNotification = (notificationId: string, notification: EventNotification): Promise<unknown> =>
  EventsNotifications.update(encodeURIComponent(notificationId), notification).then(
    (response: unknown) => {
      UserNotification.success(
        i18n.t('messages.updated', { ns: 'event_notifications' }),
        i18n.t('messages.updated_title', { ns: 'event_notifications', title: notification.title }),
      );

      return response;
    },
    (error: SubmitError) => {
      if (error.status !== 400 || !error.additional.body || !error.additional.body.failed) {
        UserNotification.error(
          i18n.t('messages.update_failed', {
            ns: 'event_notifications',
            title: notification.title,
            error: String(error),
          }),
          i18n.t('messages.update_failed_title', { ns: 'event_notifications' }),
        );
      }

      throw error;
    },
  );

export const deleteEventNotification = (notification: EventNotification): Promise<unknown> =>
  EventsNotifications.remove(encodeURIComponent(notification.id)).then(
    (response: unknown) => {
      UserNotification.success(
        i18n.t('messages.deleted', { ns: 'event_notifications' }),
        i18n.t('messages.deleted_title', { ns: 'event_notifications', title: notification.title }),
      );

      return response;
    },
    (error: SubmitError) => {
      UserNotification.error(
        i18n.t('messages.delete_failed', {
          ns: 'event_notifications',
          title: notification.title,
          error: String(error),
        }),
        i18n.t('messages.delete_failed_title', { ns: 'event_notifications' }),
      );

      throw error;
    },
  );

// Port of the old `test` action. Returns a CancellablePromise so callers can cancel the
// (potentially long-running) test request before navigating away.
export const testEventNotification = (notification: EventNotification): CancellablePromise<unknown> =>
  CancellablePromise.of(EventsNotifications.test(notification));

// Port of the old `testPersisted` action.
export const testPersistedEventNotification = (notification: EventNotification): Promise<unknown> =>
  EventsNotifications.testBynotificationId(encodeURIComponent(notification.id));

// Query key for the event notifications overview table (PaginatedEntityTable), which supplies
// its own (notifying) queryFn. Kept distinct from the other hooks' keys to avoid sharing a
// query key between different queryFns.
export const keyFn = (searchParams?: SearchParams | undefined) => [
  ...EVENT_NOTIFICATIONS_QUERY_KEY,
  'overview-paginated',
  ...(searchParams ? [searchParams] : []),
];

type Options = {
  enabled: boolean;
};

export const useEventNotificationsPaginated = (
  searchParams: SearchParams,
  { enabled }: Options = { enabled: true },
): {
  data: EventNotificationsResult | undefined;
  refetch: () => void;
  isInitialLoading: boolean;
} => {
  const { data, refetch, isInitialLoading } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError(
        fetchEventNotifications(searchParams),
        i18n.t('messages.load_all_failed', { ns: 'event_notifications' }),
        i18n.t('messages.load_all_failed_title', { ns: 'event_notifications' }),
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

// Replacement for the old `EventNotificationsActions.listAll` / `EventNotificationsStore.all`
// store state. Fetches all event notifications (unpaginated).
export const useEventNotifications = () =>
  useQuery({
    queryKey: [...EVENT_NOTIFICATIONS_QUERY_KEY, 'all'],
    queryFn: fetchAllEventNotifications,
  });

export const useLegacyEventNotificationTypes = () =>
  useQuery({
    // Deliberately not under EVENT_NOTIFICATIONS_QUERY_KEY: these are static plugin type
    // descriptors, not event notification entity data, so they should not be refetched on
    // event notification mutations.
    queryKey: ['event-notification-legacy-types'],
    queryFn: fetchLegacyEventNotificationTypes,
  });

export const useEventNotification = (notificationId: string) =>
  useQuery({
    queryKey: [...EVENT_NOTIFICATIONS_QUERY_KEY, 'id', notificationId],

    queryFn: () =>
      defaultOnError(
        getEventNotification(notificationId),
        i18n.t('messages.load_failed', { ns: 'event_notifications' }),
        i18n.t('messages.load_failed_title', { ns: 'event_notifications' }),
      ),
    retry: false,
  });

export default useEventNotificationsPaginated;
