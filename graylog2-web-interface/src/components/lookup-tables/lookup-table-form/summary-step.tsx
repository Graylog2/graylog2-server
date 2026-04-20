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
import * as React from 'react';
import styled from 'styled-components';
import { useFormikContext } from 'formik';

import { Spinner } from 'components/common';
import { Alert } from 'components/bootstrap';
import { Col, Row } from 'components/lookup-tables/layout-componets';
import { ErrorsProvider } from 'components/lookup-tables/contexts/ErrorsContext';
import ErrorsConsumer from 'components/lookup-tables/lookup-table-list/errors-consumer';
import LookupTableDetails from 'components/lookup-tables/lookup-table-view/basic-details';
import PurgeCache from 'components/lookup-tables/lookup-table-view/purge-cache';
import TestLookup from 'components/lookup-tables/lookup-table-view/test-lookup';
import { useFetchCache, useFetchDataAdapter } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import Cache from 'components/lookup-tables/Cache';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

const StyledContainer = styled(Row)`
  max-width: 1500px;
  margin: 0 auto;

  & .content.row {
    width: 100%;
    margin: 0;
    padding: 0;
    box-shadow: none;

    [class*='col-'] {
      padding: 0;
      margin: 0;
    }
  }
`;

const DangerText = styled.i`
  color: ${({ theme }) => theme.colors.variant.danger};
`;

const MuttedText = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
  width: 50px;
`;

function TableSummary({ table }: { table: LookupTable }) {
  return (
    <Col $gap="lg">
      <Col $gap="sm">
        <h2>Lookup Table Details</h2>
        <Row>
          <MuttedText>Title:</MuttedText>
          <span>{table.title || <DangerText>No title set (required)</DangerText>}</span>
        </Row>
        <Row>
          <MuttedText>Name:</MuttedText>
          <span>{table.name || <DangerText>No name set (required)</DangerText>}</span>
        </Row>
      </Col>
      <LookupTableDetails table={table} />
      {table.id && (
        <>
          <PurgeCache table={table} />
          <TestLookup table={table} />
        </>
      )}
    </Col>
  );
}

function CacheSummary({ cache }: { cache: LookupTableCache }) {
  if (!cache)
    return (
      <Alert style={{ width: '100%' }} bsStyle="danger">
        No cache selected.
      </Alert>
    );

  return <Cache cache={cache} noEdit />;
}

function AdapterSummary({ adapter }: { adapter: LookupTableAdapter }) {
  if (!adapter)
    return (
      <Alert style={{ width: '100%' }} bsStyle="danger">
        No data adapter selected.
      </Alert>
    );

  return <DataAdapter dataAdapter={adapter} noEdit />;
}

function SummaryStep() {
  const { values } = useFormikContext<LookupTable>();
  const { cache, loadingCache } = useFetchCache(values.cache_id);
  const { dataAdapter, loadingDataAdapter } = useFetchDataAdapter(values.data_adapter_id);

  return (
    <ErrorsProvider>
      <ErrorsConsumer
        lutNames={values.name ? [values.name] : undefined}
        cacheNames={cache?.name ? [cache.name] : undefined}
        adapterNames={dataAdapter?.name ? [dataAdapter.name] : undefined}
      />
      <StyledContainer $gap="xl">
        <TableSummary table={values} />
        <Col $gap="lg">
          {loadingCache ? (
            <Spinner text="Loading cache..." />
          ) : (
            <Col $gap="sm">
              <h2>Cache Details</h2>
              <CacheSummary cache={cache} />
            </Col>
          )}
          {loadingDataAdapter ? (
            <Spinner text="Loading data adapter..." />
          ) : (
            <Col $gap="sm">
              <h2>Data Adapter Details</h2>
              <AdapterSummary adapter={dataAdapter} />
            </Col>
          )}
        </Col>
      </StyledContainer>
    </ErrorsProvider>
  );
}

export default SummaryStep;
