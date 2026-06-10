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
import { useMemo } from 'react';
import styled from 'styled-components';

import { Col as BSCol, Alert } from 'components/bootstrap';
import { Col, Row, DataWell, RowContainer } from 'components/lookup-tables/layout-componets';
import useScopePermissions from 'hooks/useScopePermissions';
import Cache from 'components/lookup-tables/Cache';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';

import LookupTableDetails from './basic-details';
import PurgeCache from './purge-cache';
import TestLookup from './test-lookup';

export const Description = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
`;

type Props = {
  table: LookupTable;
  cache?: LookupTableCache;
  dataAdapter?: LookupTableAdapter;
};

function LookupTableShow({ table, cache = undefined, dataAdapter = undefined }: Props) {
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(table);

  const canEdit = useMemo(
    () => !loadingScopePermissions && scopePermissions.is_mutable,
    [loadingScopePermissions, scopePermissions],
  );

  return (
    <RowContainer $gap="xl" $withDocs $justify="center">
      <Col $gap="lg" $width="50%" style={{ flexShrink: 0 }}>
        <LookupTableDetails table={table} canEdit={canEdit} />
        <Col $gap="xs">
          <h2>Attached</h2>
          <DataWell style={{ overflow: 'auto' }}>
            <Col $gap="xs">
              <Row>
                <span style={{ width: 100, flexShrink: 0 }}>Cache</span>
                <Row $justify="space-between" $align="center">
                  {cache ? (
                    <span aria-label="cache details">{cache.title}</span>
                  ) : (
                    <i>
                      <Description>No cache</Description>
                    </i>
                  )}
                </Row>
              </Row>
              <Row>
                <span style={{ width: 100, flexShrink: 0 }}>Data Adapter</span>
                <Row $justify="space-between" $align="center">
                  {dataAdapter ? (
                    <span aria-label="adapter details">{dataAdapter.title}</span>
                  ) : (
                    <i>
                      <Description>No data adapter</Description>
                    </i>
                  )}
                </Row>
              </Row>
            </Col>
          </DataWell>
        </Col>
        <PurgeCache table={table} />
        <TestLookup table={table} />
      </Col>
      <Col $gap="lg" $width="50%">
        <Col $gap="sm">
          <h2>Cache</h2>
          <BSCol style={{ width: '100%' }}>
            {cache ? <Cache cache={cache} /> : <Alert bsStyle="info">No cache set</Alert>}
          </BSCol>
        </Col>
        <Col $gap="sm">
          <h2>Data Adapter</h2>
          <BSCol style={{ width: '100%' }}>
            {dataAdapter ? (
              <DataAdapter dataAdapter={dataAdapter} />
            ) : (
              <Alert bsStyle="info">No data adapter set</Alert>
            )}
          </BSCol>
        </Col>
      </Col>
    </RowContainer>
  );
}

export default LookupTableShow;
