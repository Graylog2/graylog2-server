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
import React from 'react';

import { Alert, Button } from 'components/bootstrap';
import { Spinner } from 'components/common';

import DataNodeConfigurationForm from './DataNodeConfigurationForm';
import { getConfigurationCategories } from './configurationFields';
import useManagedConfiguration from './useManagedConfiguration';

type Props = React.PropsWithChildren<{ basePath: string; nodeId?: string | null }>;

const DataNodeConfigurationEditor = ({ basePath, nodeId = null, children = undefined }: Props) => {
  const { data, isLoading, isFetching, error, refetch } = useManagedConfiguration(nodeId);

  if (isLoading) return <Spinner text="Loading Data Node configuration..." />;

  const loadError = error && (
    <Alert bsStyle="danger">
      <p>
        {data
          ? 'Could not refresh Data Node configuration. Your changes have been kept. Please try again.'
          : 'Could not load Data Node configuration. Please try again.'}
      </p>
      <Button type="button" bsSize="small" disabled={isFetching} onClick={() => refetch()}>
        Retry
      </Button>
    </Alert>
  );

  if (error && !data) return loadError;

  const categories = getConfigurationCategories(data?.parameters ?? []);

  return (
    <>
      {loadError}
      {categories.length === 0 ? (
        <Alert bsStyle="info">No Data Node configuration parameters are available here.</Alert>
      ) : (
        <DataNodeConfigurationForm
          key={nodeId ?? 'cluster'}
          categories={categories}
          nodeId={nodeId}
          basePath={basePath}>
          {children}
        </DataNodeConfigurationForm>
      )}
    </>
  );
};

export default DataNodeConfigurationEditor;
