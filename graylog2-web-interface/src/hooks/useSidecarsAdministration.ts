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
import upperFirst from 'lodash/upperFirst';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { SidecarSummary } from 'components/sidecars/types';

const SOURCE_URL = '/sidecar';
const QUERY_KEY = ['sidecars', 'administration'] as const;

type ListParams = {
  query?: string;
  page?: number;
  pageSize?: number;
  filters?: {};
};

type ListResponse = {
  sidecars: Array<SidecarSummary>;
  query: string;
  filters: {};
  pagination: {
    total: number;
    count: number;
    page: number;
    per_page: number;
  };
};

const fetchSidecarsAdministration = ({
  query = '',
  page = 1,
  pageSize = 50,
  filters,
}: ListParams): Promise<ListResponse> =>
  fetch('POST', qualifyUrl(`${SOURCE_URL}/administration`), {
    query,
    page,
    per_page: pageSize,
    filters,
  });

const useSidecarsAdministration = ({ query = '', page = 1, pageSize = 50, filters }: ListParams) =>
  useQuery({
    queryKey: [...QUERY_KEY, { query, page, pageSize, filters }],
    queryFn: () =>
      fetchSidecarsAdministration({ query, page, pageSize, filters }).catch((error) => {
        UserNotification.error(
          error.status === 400 ? error.responseMessage : `Fetching Sidecars failed with status: ${error.message}`,
          'Could not retrieve Sidecars',
        );

        throw error;
      }),
    select: (response: ListResponse) => ({
      sidecars: response.sidecars,
      query: response.query,
      filters: response.filters,
      pagination: {
        total: response.pagination.total,
        count: response.pagination.count,
        page: response.pagination.page,
        pageSize: response.pagination.per_page,
        perPage: response.pagination.per_page,
      },
    }),
    refetchInterval: 5000,
  });

export const useSetSidecarAction = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ action, collectors }: { action: string; collectors: { [sidecarId: string]: string[] } }) => {
      const sidecarIds = Object.keys(collectors);
      const formattedCollectors = sidecarIds.map((sidecarId) => ({
        sidecar_id: sidecarId,
        collector_ids: collectors[sidecarId],
      }));

      return fetch('PUT', qualifyUrl(`${SOURCE_URL}/administration/action`), {
        action,
        collectors: formattedCollectors,
      });
    },
    onSuccess: (_data, { action, collectors }) => {
      const formattedCollectorsCount = Object.keys(collectors).length;
      UserNotification.success('', `${upperFirst(action)} for ${formattedCollectorsCount} collectors requested`);
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
    onError: (error: { message?: string }, { action }) => {
      UserNotification.error(`Requesting ${action} failed with status: ${error}`, `Could not ${action} collectors`);
    },
  });
};

export default useSidecarsAdministration;
