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
import merge from 'lodash/merge';
import { useQuery } from '@tanstack/react-query';

import { SidecarCollectors } from '@graylog/server-api';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { Collector } from 'components/sidecars/types';
import i18n from 'i18n';
import translateApiError from 'util/translateApiError';

const SOURCE_URL = '/sidecar';
export const COLLECTORS_QUERY_KEY = ['collectors'] as const;

type ListResponse = {
  collectors: Array<Collector>;
  query: string;
  total: number;
  pagination: {
    page: number;
    per_page: number;
    total: number;
  };
};

type PaginatedCollectorsState = {
  paginatedCollectors: Array<Collector>;
  query: string;
  total: number;
  pagination: {
    page: number;
    pageSize: number;
    total: number;
  };
};

const _fetch = ({ query, page, pageSize }: { query?: string; page?: number; pageSize?: number }) =>
  SidecarCollectors.listSummary(undefined, page, pageSize, query);

export const fetchCollectorsAll = (): Promise<{ collectors: Array<Collector> }> => {
  const promise = _fetch({ pageSize: 0 });

  promise.catch((error) => {
    UserNotification.error(
      i18n.t('use_collectors_notifications.fetch_failed', { ns: 'collectors', error }),
      i18n.t('use_collectors_notifications.fetch_failed_title', { ns: 'collectors' }),
    );
  });

  return promise;
};

export const fetchCollectorsPaginated = ({
  query = '',
  page = 1,
  pageSize = 10,
}: {
  query?: string;
  page?: number;
  pageSize?: number;
}): Promise<ListResponse> => {
  const promise = _fetch({ query, page, pageSize });

  promise.catch((error) => {
    UserNotification.error(
      i18n.t('use_collectors_notifications.fetch_failed', { ns: 'collectors', error }),
      i18n.t('use_collectors_notifications.fetch_failed_title', { ns: 'collectors' }),
    );
  });

  return promise;
};

export const fetchCollector = (collectorId: string): Promise<Collector> => {
  const promise = SidecarCollectors.getCollector(collectorId);

  promise.catch((error) => {
    let errorMessage = i18n.t('use_collectors_notifications.fetch_collector_failed', { ns: 'collectors', error });

    if (error.status === 404) {
      errorMessage = i18n.t('use_collectors_notifications.not_found', { ns: 'collectors', collectorId });
    }

    UserNotification.error(
      errorMessage,
      i18n.t('use_collectors_notifications.fetch_collector_failed_title', { ns: 'collectors' }),
    );
  });

  return promise;
};

export const createCollector = (collector: Collector): Promise<unknown> => {
  const promise = fetch('POST', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors`), collector);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collectors_notifications.created_success', { ns: 'collectors' })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collectors_notifications.fetch_failed', { ns: 'collectors', error }),
        i18n.t('use_collectors_notifications.fetch_failed_title', { ns: 'collectors' }),
      );
    },
  );

  return promise;
};

export const updateCollector = (collector: Collector): Promise<unknown> => {
  const promise = fetch('PUT', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/${collector.id}`), collector);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collectors_notifications.updated_success', { ns: 'collectors' })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collectors_notifications.fetch_failed', { ns: 'collectors', error }),
        i18n.t('use_collectors_notifications.fetch_failed_title', { ns: 'collectors' }),
      );
    },
  );

  return promise;
};

export const deleteCollector = (collector: Collector): Promise<unknown> => {
  const promise = SidecarCollectors.deleteCollector(collector.id);

  promise.then(
    () =>
      UserNotification.success(
        '',
        i18n.t('use_collectors_notifications.deleted_success', { ns: 'collectors', name: collector.name }),
      ),
    (error) => {
      UserNotification.error(
        i18n.t('use_collectors_notifications.delete_failed', {
          ns: 'collectors',
          error: error.status === 400 ? (translateApiError(error) ?? error.responseMessage) : error.message,
        }),
        i18n.t('use_collectors_notifications.delete_failed_title', { ns: 'collectors', name: collector.name }),
      );
    },
  );

  return promise;
};

export const copyCollector = (collectorId: string, name: string): Promise<unknown> => {
  const promise = SidecarCollectors.copyCollector(collectorId, name);

  promise.then(
    () =>
      UserNotification.success('', i18n.t('use_collectors_notifications.copied_success', { ns: 'collectors', name })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collectors_notifications.copy_failed', { ns: 'collectors', name, error: error.message }),
        i18n.t('use_collectors_notifications.copy_failed_title', { ns: 'collectors' }),
      );
    },
  );

  return promise;
};

export const validateCollector = (collector: Collector): Promise<{ errors: { name: string[] }; failed: boolean }> => {
  const payload: Partial<Collector> = {
    id: ' ',
    service_type: 'exec',
    executable_path: ' ',
    default_template: ' ',
  };

  merge(payload, collector);

  const promise = fetch('POST', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/validate`), payload);

  promise.then(
    (response) => response,
    (error) =>
      UserNotification.error(
        i18n.t('use_collectors_notifications.validate_failed', {
          ns: 'collectors',
          name: payload.name,
          error: error.message,
        }),
        i18n.t('use_collectors_notifications.validate_failed_title', { ns: 'collectors' }),
      ),
  );

  return promise;
};

export const useCollectorsAll = () =>
  useQuery({
    queryKey: [...COLLECTORS_QUERY_KEY, 'all'],
    queryFn: fetchCollectorsAll,
    select: (response) => response.collectors,
  });

export const useCollectorsPaginated = ({
  query = '',
  page = 1,
  pageSize = 10,
}: {
  query?: string;
  page?: number;
  pageSize?: number;
}) =>
  useQuery({
    queryKey: [...COLLECTORS_QUERY_KEY, 'paginated', { query, page, pageSize }],
    queryFn: () => fetchCollectorsPaginated({ query, page, pageSize }),
    select: (response): PaginatedCollectorsState => ({
      paginatedCollectors: response.collectors,
      query: response.query,
      pagination: {
        page: response.pagination.page,
        pageSize: response.pagination.per_page,
        total: response.pagination.total,
      },
      total: response.total,
    }),
  });
