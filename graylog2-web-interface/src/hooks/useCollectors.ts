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
import URI from 'urijs';
import merge from 'lodash/merge';
import { useQuery } from '@tanstack/react-query';

import * as URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { Collector } from 'components/sidecars/types';

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

const _fetch = ({ query, page, pageSize }: { query?: string; page?: number; pageSize?: number }) => {
  const search = {
    query,
    page,
    per_page: pageSize,
  };

  const uri = URI(`${SOURCE_URL}/collectors/summary`).search(search).toString();

  return fetch('GET', URLUtils.qualifyUrl(uri));
};

export const fetchCollectorsAll = (): Promise<{ collectors: Array<Collector> }> => {
  const promise = _fetch({ pageSize: 0 });

  promise.catch((error) => {
    UserNotification.error(`Fetching collectors failed with status: ${error}`, 'Could not retrieve collectors');
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
    UserNotification.error(`Fetching collectors failed with status: ${error}`, 'Could not retrieve collectors');
  });

  return promise;
};

export const fetchCollector = (collectorId: string): Promise<Collector> => {
  const promise = fetch('GET', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/${collectorId}`));

  promise.catch((error) => {
    let errorMessage = `Fetching Collector failed with status: ${error}`;

    if (error.status === 404) {
      errorMessage = `Unable to find a collector with ID <${collectorId}>, please ensure it was not deleted.`;
    }

    UserNotification.error(errorMessage, 'Could not retrieve Collector');
  });

  return promise;
};

export const createCollector = (collector: Collector): Promise<unknown> => {
  const promise = fetch('POST', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors`), collector);

  promise.then(
    () => UserNotification.success('', 'Collector successfully created'),
    (error) => {
      UserNotification.error(`Fetching collectors failed with status: ${error}`, 'Could not retrieve collectors');
    },
  );

  return promise;
};

export const updateCollector = (collector: Collector): Promise<unknown> => {
  const promise = fetch('PUT', URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/${collector.id}`), collector);

  promise.then(
    () => UserNotification.success('', 'Collector successfully updated'),
    (error) => {
      UserNotification.error(`Fetching collectors failed with status: ${error}`, 'Could not retrieve collectors');
    },
  );

  return promise;
};

export const deleteCollector = (collector: Collector): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/${collector.id}`);
  const promise = fetch('DELETE', url);

  promise.then(
    () => UserNotification.success('', `Collector "${collector.name}" successfully deleted`),
    (error) => {
      UserNotification.error(
        `Deleting Collector failed: ${error.status === 400 ? error.responseMessage : error.message}`,
        `Could not delete Collector "${collector.name}"`,
      );
    },
  );

  return promise;
};

export const copyCollector = (collectorId: string, name: string): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/collectors/${collectorId}/${name}`);
  const promise = fetch('POST', url);

  promise.then(
    () => UserNotification.success('', `Collector "${name}" successfully copied`),
    (error) => {
      UserNotification.error(
        `Saving collector "${name}" failed with status: ${error.message}`,
        'Could not save Collector',
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
        `Validating collector "${payload.name}" failed with status: ${error.message}`,
        'Could not validate collector',
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
