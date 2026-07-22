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

import {
  COLLECTORS_QUERY_KEY,
  useCollectorsPaginated,
  copyCollector,
  deleteCollector,
  validateCollector,
} from 'hooks/useCollectors';
import { Spinner } from 'components/common';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import CollectorList from './CollectorList';

const CollectorListContainer = () => {
  const queryClient = useQueryClient();
  const sendTelemetry = useSendTelemetry();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const { data: collectors } = useCollectorsPaginated({ query, page, pageSize });

  const invalidateCollectors = () => queryClient.invalidateQueries({ queryKey: COLLECTORS_QUERY_KEY });

  const handleClone = (collectorId: string, name: string, callback: () => void) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.LOG_COLLECTOR_CLONED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    copyCollector(collectorId, name).then(() => {
      invalidateCollectors();
      callback();
    });
  };

  const handleDelete = async (collector) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS.LOG_COLLECTOR_DELETED, {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    await deleteCollector(collector);
    invalidateCollectors();
  };

  const handlePageChange = (newPage: number, newPageSize: number) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  const handleQueryChange = (newQuery: string = '', callback: () => void = () => {}) => {
    setQuery(newQuery);
    setPage(1);
    callback();
  };

  if (!collectors || !collectors.paginatedCollectors) {
    return <Spinner />;
  }

  return (
    <CollectorList
      collectors={collectors.paginatedCollectors}
      pagination={collectors.pagination}
      query={collectors.query}
      total={collectors.total}
      onPageChange={handlePageChange}
      onQueryChange={handleQueryChange}
      onClone={handleClone}
      onDelete={handleDelete}
      validateCollector={validateCollector}
    />
  );
};

export default CollectorListContainer;
