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
import React, { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { Spinner } from 'components/common';
import { useCollectorsAll } from 'hooks/useCollectors';
import {
  COLLECTOR_CONFIGURATIONS_QUERY_KEY,
  useCollectorConfigurationsPaginated,
  copyConfiguration,
  deleteConfiguration,
  validateConfiguration,
} from 'hooks/useCollectorConfigurations';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { Configuration } from 'components/sidecars/types';

import ConfigurationList from './ConfigurationList';

const ConfigurationListContainer = () => {
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { data: collectors } = useCollectorsAll();
  const { data: configurations } = useCollectorConfigurationsPaginated({ query, page, pageSize });

  const invalidateConfigurations = () =>
    queryClient.invalidateQueries({ queryKey: COLLECTOR_CONFIGURATIONS_QUERY_KEY });

  const handleDelete = (configuration: Configuration) =>
    deleteConfiguration(configuration).then(invalidateConfigurations);

  const handlePageChange = (newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  const handleQueryChange = (newQuery: string = '', callback: () => void = () => {}) => {
    setQuery(newQuery);
    setPage(1);
    callback();
  };

  const handleClone = (configurationId: string, name: string, callback: () => void) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.CONFIGURATION_CLONED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    copyConfiguration(configurationId, name).then((response) => {
      invalidateConfigurations();
      callback();

      return response;
    });
  };

  const isLoading = !collectors || !configurations || !configurations.paginatedConfigurations;

  if (isLoading) {
    return <Spinner />;
  }

  return (
    <ConfigurationList
      collectors={collectors}
      query={configurations.query}
      pagination={configurations.pagination}
      total={configurations.total}
      configurations={configurations.paginatedConfigurations}
      onPageChange={handlePageChange}
      onQueryChange={handleQueryChange}
      onClone={handleClone}
      onDelete={handleDelete}
      validateConfiguration={validateConfiguration}
    />
  );
};

export default ConfigurationListContainer;
