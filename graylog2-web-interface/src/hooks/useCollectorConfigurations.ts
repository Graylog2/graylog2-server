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
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import type { Configuration, ConfigurationSidecarsResponse } from 'components/sidecars/types';

const SOURCE_URL = '/sidecar';
export const COLLECTOR_CONFIGURATIONS_QUERY_KEY = ['collector-configurations'] as const;

type ListResponse = {
  configurations: Array<Configuration>;
  query: string;
  total: number;
  pagination: {
    page: number;
    per_page: number;
    total: number;
  };
};

type PaginatedConfigurationsState = {
  paginatedConfigurations: Array<Configuration>;
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

  const uri = URI(`${SOURCE_URL}/configurations`).search(search).toString();

  return fetch('GET', URLUtils.qualifyUrl(uri));
};

export const fetchAllConfigurations = (): Promise<{ configurations: Array<Configuration> }> => {
  const promise = _fetch({ pageSize: 0 });

  promise.catch((error) => {
    UserNotification.error(
      `Fetching collector configurations failed with status: ${error}`,
      'Could not retrieve configurations',
    );
  });

  return promise;
};

export const fetchConfigurationsPaginated = ({
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
      `Fetching collector configurations failed with status: ${error}`,
      'Could not retrieve configurations',
    );
  });

  return promise;
};

export const fetchConfiguration = (configurationId: string): Promise<Configuration> => {
  const promise = fetch('GET', URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/${configurationId}`));

  promise.catch((error) => {
    let errorMessage = `Fetching Configuration failed with status: ${error}`;

    if (error.status === 404) {
      errorMessage = `Unable to find a Configuration with ID <${configurationId}>, please ensure it was not deleted.`;
    }

    UserNotification.error(errorMessage, 'Could not retrieve Configuration');
  });

  return promise;
};

export const fetchConfigurationSidecars = (configurationId: string): Promise<ConfigurationSidecarsResponse> => {
  const promise = fetch('GET', URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/${configurationId}/sidecars`));

  promise.catch((error) => {
    let errorMessage = `Fetching Configuration failed with status: ${error}`;

    if (error.status === 404) {
      errorMessage = `Unable to find a Configuration with ID <${configurationId}>, please ensure it was not deleted.`;
    }

    UserNotification.error(errorMessage, 'Could not retrieve Configuration');
  });

  return promise;
};

export const renderConfigurationPreview = (template: string): Promise<{ preview: string }> => {
  const promise = fetch('POST', URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/render/preview`), { template });

  promise.catch((error) => {
    UserNotification.error(`Fetching configuration preview failed with status: ${error}`, 'Could not retrieve preview');
  });

  return promise;
};

export const createConfiguration = (configuration: Configuration): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/configurations`);
  const promise = fetch('POST', url, configuration);

  promise.then(
    () => UserNotification.success('', 'Configuration successfully created'),
    (error) => {
      UserNotification.error(
        error.status === 400 ? error.responseMessage : `Creating configuration failed with status: ${error.message}`,
        'Could not save configuration',
      );
    },
  );

  return promise;
};

export const updateConfiguration = (configuration: Configuration): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/${configuration.id}`);
  const promise = fetch('PUT', url, configuration);

  promise.then(
    () => UserNotification.success('', 'Configuration successfully updated'),
    (error) => {
      UserNotification.error(
        `Updating Configuration failed: ${error.status === 400 ? error.responseMessage : error.message}`,
        `Could not update Configuration ${configuration.name}`,
      );
    },
  );

  return promise;
};

export const copyConfiguration = (configurationId: string, name: string): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/${configurationId}/${name}`);
  const promise = fetch('POST', url);

  promise.then(
    () => UserNotification.success('', `Configuration "${name}" successfully copied`),
    (error) => {
      UserNotification.error(
        `Saving configuration "${name}" failed with status: ${error.message}`,
        'Could not save Configuration',
      );
    },
  );

  return promise;
};

export const deleteConfiguration = (configuration: Configuration): Promise<unknown> => {
  const url = URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/${configuration.id}`);
  const promise = fetch('DELETE', url);

  promise.then(
    () => UserNotification.success('', `Configuration "${configuration.name}" successfully deleted`),
    (error) => {
      UserNotification.error(
        `Deleting Configuration failed: ${error.status === 400 ? error.responseMessage : error.message}`,
        `Could not delete Configuration ${configuration.name}`,
      );
    },
  );

  return promise;
};

export const validateConfiguration = (
  configuration: Partial<Configuration>,
): Promise<{ errors: { name: string[] }; failed: boolean }> => {
  const payload = {
    name: ' ',
    collector_id: ' ',
    color: ' ',
    template: ' ',
  };

  merge(payload, configuration);

  const promise = fetch('POST', URLUtils.qualifyUrl(`${SOURCE_URL}/configurations/validate`), payload);

  promise.then(
    (response) => response,
    (error) =>
      UserNotification.error(
        `Validating configuration "${payload.name}" failed with status: ${error.message}`,
        'Could not validate configuration',
      ),
  );

  return promise;
};

export const useAllCollectorConfigurations = () =>
  useQuery({
    queryKey: [...COLLECTOR_CONFIGURATIONS_QUERY_KEY, 'all'],
    queryFn: fetchAllConfigurations,
    select: (response) => response.configurations,
  });

export const useCollectorConfigurationsPaginated = ({
  query = '',
  page = 1,
  pageSize = 10,
}: {
  query?: string;
  page?: number;
  pageSize?: number;
}) =>
  useQuery({
    queryKey: [...COLLECTOR_CONFIGURATIONS_QUERY_KEY, 'paginated', { query, page, pageSize }],
    queryFn: () => fetchConfigurationsPaginated({ query, page, pageSize }),
    select: (response): PaginatedConfigurationsState => ({
      paginatedConfigurations: response.configurations,
      query: response.query,
      pagination: {
        page: response.pagination.page,
        pageSize: response.pagination.per_page,
        total: response.pagination.total,
      },
      total: response.total,
    }),
  });
