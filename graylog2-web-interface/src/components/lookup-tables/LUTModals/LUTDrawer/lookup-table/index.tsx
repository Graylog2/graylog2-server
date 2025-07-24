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

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { Link } from 'components/common/router';
import { Col, Row, DataWell } from 'components/lookup-tables/layout-componets';
import useScopePermissions from 'hooks/useScopePermissions';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';
import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';

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
  cache: LookupTableCache;
  dataAdapter: LookupTableAdapter;
};

function LookupTableDetails({ table, cache, dataAdapter }: Props) {
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(table);
  const { setModal, setTitle, setEntity } = useModalContext();

  const handleEdit = () => {
    setModal('LUT-EDIT');
    setTitle(table.name);
    setEntity(table);
  }

  return (
    <Col $gap="lg">
      <Col $gap="xs">
        <Row $align="flex-end" $justify="space-between">
          <h2>Description</h2>
          {!loadingScopePermissions && scopePermissions?.is_mutable && (
            <Button bsStyle="primary" bsSize="sm" onClick={handleEdit} name="edit_square">
              Edit
            </Button>
          )}
        </Row>
        <Description>{table.description}</Description>
      </Col>
      {(table.default_single_value || table.default_multi_value) && (
        <DataWell>
          <Col $gap="xs">
            {table.default_single_value && (
              <Row>
                <span style={{ width: 208 }}>Default single value</span>
                <Row $gap="md">
                  <code>{table.default_single_value}</code>
                  <span>
                    <Description>({table.default_single_value_type.toLowerCase()})</Description>
                  </span>
                </Row>
              </Row>
            )}
            {table.default_multi_value && (
              <Row>
                <span style={{ width: 208 }}>Default multi value</span>
                <Row $gap="md">
                  <code>{table.default_multi_value}</code>
                  <span>
                    <Description>({table.default_multi_value_type.toLowerCase()})</Description>
                  </span>
                </Row>
              </Row>
            )}
          </Col>
        </DataWell>
      )}
      <Col $gap="xs">
        <h2>Attached</h2>
        <DataWell>
          <Col $gap="xs">
            <Row>
              <span style={{ width: 100 }}>Cache</span>
              <Link to={Routes.SYSTEM.LOOKUPTABLES.CACHES.show(cache.name)}>{cache.title}</Link>
            </Row>
            <Row>
              <span style={{ width: 100 }}>Data Adapter</span>
              <Link to={Routes.SYSTEM.LOOKUPTABLES.DATA_ADAPTERS.show(dataAdapter.name)}>{dataAdapter.title}</Link>
            </Row>
          </Col>
        </DataWell>
      </Col>
      <PurgeCache table={table} />
      <TestLookup table={table} />
    </Col>
  );
}

export default LookupTableDetails;
