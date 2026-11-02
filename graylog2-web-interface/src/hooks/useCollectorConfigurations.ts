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

import { SidecarConfigurations } from '@graylog/server-api';

import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import type { Configuration, ConfigurationSidecarsResponse } from 'components/sidecars/types';
import i18n from 'i18n';

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
      i18n.t('use_collector_configurations_notifications.fetch_failed', { ns: 'collectors', error }),
      i18n.t('use_collector_configurations_notifications.fetch_failed_title', { ns: 'collectors' }),
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
      i18n.t('use_collector_configurations_notifications.fetch_failed', { ns: 'collectors', error }),
      i18n.t('use_collector_configurations_notifications.fetch_failed_title', { ns: 'collectors' }),
    );
  });

  return promise;
};

export const fetchConfiguration = (configurationId: string): Promise<Configuration> => {
  const promise = SidecarConfigurations.getConfigurations(configurationId);

  promise.catch((error) => {
    let errorMessage = i18n.t('use_collector_configurations_notifications.fetch_configuration_failed', { ns: 'collectors', error });

    if (error.status === 404) {
      errorMessage = i18n.t('use_collector_configurations_notifications.configuration_not_found', { ns: 'collectors', configurationId });
    }

    UserNotification.error(errorMessage, i18n.t('use_collector_configurations_notifications.fetch_configuration_failed_title', { ns: 'collectors' }));
  });

  return promise;
};

export const fetchConfigurationSidecars = (configurationId: string): Promise<ConfigurationSidecarsResponse> => {
  const promise = SidecarConfigurations.getConfigurationSidecars(configurationId);

  promise.catch((error) => {
    let errorMessage = i18n.t('use_collector_configurations_notifications.fetch_configuration_failed', { ns: 'collectors', error });

    if (error.status === 404) {
      errorMessage = i18n.t('use_collector_configurations_notifications.configuration_not_found', { ns: 'collectors', configurationId });
    }

    UserNotification.error(errorMessage, i18n.t('use_collector_configurations_notifications.fetch_configuration_failed_title', { ns: 'collectors' }));
  });

  return promise;
};

export const renderConfigurationPreview = (template: string): Promise<{ preview: string }> => {
  const promise = SidecarConfigurations.renderConfiguration({ template });

  promise.catch((error) => {
    UserNotification.error(i18n.t('use_collector_configurations_notifications.preview_failed', { ns: 'collectors', error }), i18n.t('use_collector_configurations_notifications.preview_failed_title', { ns: 'collectors' }));
  });

  return promise;
};

export const createConfiguration = (configuration: Configuration): Promise<unknown> => {
  const promise = SidecarConfigurations.createConfiguration(configuration);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collector_configurations_notifications.created_success', { ns: 'collectors' })),
    (error) => {
      UserNotification.error(
        error.status === 400 ? error.responseMessage : i18n.t('use_collector_configurations_notifications.create_failed', { ns: 'collectors', error: error.message }),
        i18n.t('use_collector_configurations_notifications.create_failed_title', { ns: 'collectors' }),
      );
    },
  );

  return promise;
};

export const updateConfiguration = (configuration: Configuration): Promise<unknown> => {
  const promise = SidecarConfigurations.updateConfiguration(configuration.id, configuration);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collector_configurations_notifications.updated_success', { ns: 'collectors' })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collector_configurations_notifications.update_failed', { ns: 'collectors', error: error.status === 400 ? error.responseMessage : error.message }),
        i18n.t('use_collector_configurations_notifications.update_failed_title', { ns: 'collectors', name: configuration.name }),
      );
    },
  );

  return promise;
};

export const copyConfiguration = (configurationId: string, name: string): Promise<unknown> => {
  const promise = SidecarConfigurations.copyConfiguration(configurationId, name);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collector_configurations_notifications.copied_success', { ns: 'collectors', name })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collector_configurations_notifications.copy_failed', { ns: 'collectors', name, error: error.message }),
        i18n.t('use_collector_configurations_notifications.copy_failed_title', { ns: 'collectors' }),
      );
    },
  );

  return promise;
};

export const deleteConfiguration = (configuration: Configuration): Promise<unknown> => {
  const promise = SidecarConfigurations.deleteConfiguration(configuration.id);

  promise.then(
    () => UserNotification.success('', i18n.t('use_collector_configurations_notifications.deleted_success', { ns: 'collectors', name: configuration.name })),
    (error) => {
      UserNotification.error(
        i18n.t('use_collector_configurations_notifications.delete_failed', { ns: 'collectors', error: error.status === 400 ? error.responseMessage : error.message }),
        i18n.t('use_collector_configurations_notifications.delete_failed_title', { ns: 'collectors', name: configuration.name }),
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
        i18n.t('use_collector_configurations_notifications.validate_failed', { ns: 'collectors', name: payload.name, error: error.message }),
        i18n.t('use_collector_configurations_notifications.validate_failed_title', { ns: 'collectors' }),
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
