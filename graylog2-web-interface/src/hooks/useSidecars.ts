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
import URI from 'urijs';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { SidecarSummary, SidecarCollectorPairType, Configuration } from 'components/sidecars/types';

const SOURCE_URL = '/sidecars';
const QUERY_KEY = ['sidecars', 'list'] as const;

export type PaginationOptions = {
  query: string;
  sortField?: string;
  order?: string;
  pageSize: number;
  page: number;
  onlyActive: string | boolean;
};

type ListResponse = {
  sidecars: SidecarSummary[];
  query: string;
  only_active: boolean;
  pagination: {
    total: number;
    count: number;
    page: number;
    per_page: number;
  };
  sort: string;
  order: string;
};

const fetchSidecarsList = ({
  query = '',
  page = 1,
  pageSize = 50,
  onlyActive = false,
  sortField = 'node_name',
  order = 'asc',
}: Partial<PaginationOptions>): Promise<ListResponse> => {
  const search = {
    query,
    page,
    per_page: pageSize,
    only_active: onlyActive,
    sort: sortField,
    order,
  };

  const uri = URI(SOURCE_URL).search(search).toString();

  return fetch('GET', qualifyUrl(uri));
};

export const fetchSidecar = (sidecarId: string): Promise<SidecarSummary> => {
  const promise: Promise<SidecarSummary> = fetch('GET', qualifyUrl(`${SOURCE_URL}/${sidecarId}`));

  promise.catch((error) => {
    let errorMessage = `Fetching Sidecar failed with status: ${error}`;

    if (error.status === 404) {
      errorMessage = `Unable to find a sidecar with ID <${sidecarId}>, maybe it was inactive for too long.`;
    }

    UserNotification.error(errorMessage, 'Could not retrieve Sidecar');
  });

  return promise;
};

export const fetchSidecarActions = (sidecarId: string): Promise<unknown> => {
  const promise = fetch('GET', qualifyUrl(`${SOURCE_URL}/${sidecarId}/action`));

  promise.catch((error) => {
    UserNotification.error(
      `Fetching Sidecar actions failed with status: ${error}`,
      'Could not retrieve Sidecar actions',
    );
  });

  return promise;
};

export const restartCollector = (sidecarId: string, collector: string): Promise<unknown> => {
  const action = {
    collector,
    properties: { restart: true },
  };
  const promise = fetch('PUT', qualifyUrl(`${SOURCE_URL}/${sidecarId}/action`), [action]);

  promise.catch((error) => {
    UserNotification.error(`Restarting Sidecar failed with status: ${error}`, 'Could not restart Sidecar');
  });

  return promise;
};

export const assignConfigurations = (
  sidecars: SidecarCollectorPairType[],
  configurations: Configuration[],
): Promise<unknown> => {
  const nodes = sidecars.map(({ sidecar, collector }) => {
    // Add all previous assignments, but the one that was changed
    const assignments = sidecar.assignments.filter((assignment) => assignment.collector_id !== collector.id);

    // Add new assignments
    configurations.forEach((configuration) => {
      assignments.push({ collector_id: collector.id, configuration_id: configuration.id, assigned_from_tags: [] });
    });

    return { node_id: sidecar.node_id, assignments };
  });

  const promise = fetch('PUT', qualifyUrl(`${SOURCE_URL}/configurations`), { nodes });

  promise.then(
    (response) => {
      UserNotification.success('', `Configuration change for ${sidecars.length} collectors requested`);

      return response;
    },
    (error) => {
      UserNotification.error(
        `Fetching Sidecar actions failed with status: ${error}`,
        'Could not retrieve Sidecar actions',
      );
    },
  );

  return promise;
};

export const useSidecarsListPaginated = (opts: Partial<PaginationOptions>) =>
  useQuery({
    queryKey: [...QUERY_KEY, opts],
    queryFn: () =>
      fetchSidecarsList(opts).catch((error) => {
        UserNotification.error(
          error.status === 400 ? error.responseMessage : `Fetching Sidecars failed with status: ${error.message}`,
          'Could not retrieve Sidecars',
        );

        throw error;
      }),
    select: (response: ListResponse) => ({
      sidecars: response.sidecars,
      query: response.query,
      onlyActive: response.only_active,
      pagination: {
        total: response.pagination.total,
        count: response.pagination.count,
        page: response.pagination.page,
        pageSize: response.pagination.per_page,
      },
      sort: {
        field: response.sort,
        order: response.order,
      },
    }),
    refetchInterval: 5000,
  });

export default useSidecarsListPaginated;
