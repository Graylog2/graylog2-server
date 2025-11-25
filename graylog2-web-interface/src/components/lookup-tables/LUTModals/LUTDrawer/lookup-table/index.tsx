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

import { Col as BSCol } from 'components/bootstrap';
import { Icon } from 'components/common';
import { Col, Row, DataWell } from 'components/lookup-tables/layout-componets';
import { useModalContext } from 'components/lookup-tables/contexts/ModalContext';
import useScopePermissions from 'hooks/useScopePermissions';
import Cache from 'components/lookup-tables/Cache';
import DataAdapter from 'components/lookup-tables/DataAdapter';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';

import LookupTableDetails from './lookup-table-details';
import PurgeCache from './purge-cache';
import TestLookup from './test-lookup';

export const Description = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
`;

const LinkSpan = styled.span`
  color: ${({ theme }) => theme.colors.link.default};
  cursor: pointer;
  max-width: 390px;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    color: ${({ theme }) => theme.colors.link.hover};
  }
`;

type Props = {
  table: LookupTable;
  cache: LookupTableCache;
  dataAdapter: LookupTableAdapter;
};

function LookupTableView({ table, cache, dataAdapter }: Props) {
  const { loadingScopePermissions, scopePermissions } = useScopePermissions(table);
  const { double, setDouble } = useModalContext();
  const [showAttached, setShowAttached] = React.useState<string>();

  const canEdit = React.useMemo(
    () => !loadingScopePermissions && scopePermissions.is_mutable,
    [loadingScopePermissions, scopePermissions],
  );

  const handleShowAttached = (type: string) => () => {
    setDouble(showAttached === type ? !double : true);
    setShowAttached(showAttached === type ? undefined : type);
  };

  React.useEffect(() => () => setDouble(false), [setDouble]);

  return (
    <Row $align="stretch" $gap="lg">
      <Col $gap="lg" $width={double ? '50%' : '100%'} style={{ flexShrink: 0 }}>
        <LookupTableDetails table={table} canEdit={canEdit} />
        <Col $gap="xs">
          <h2>Attached</h2>
          <DataWell style={{ overflow: 'auto' }}>
            <Col $gap="xs">
              <Row>
                <span style={{ width: 100, flexShrink: 0 }}>Cache</span>
                <Row $justify="space-between" $align="center">
                  {cache ? (
                    <>
                      <LinkSpan role="link" aria-label="cache details" onClick={handleShowAttached('cache-details')}>
                        {cache.title}
                      </LinkSpan>
                      <Description>
                        <Icon name="chevron_right" rotation={showAttached === 'cache-details' ? 180 : 0} size="sm" />
                      </Description>
                    </>
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
                    <>
                      <LinkSpan
                        role="link"
                        aria-label="adapter details"
                        onClick={handleShowAttached('adapter-details')}>
                        {dataAdapter.title}
                      </LinkSpan>
                      <Description>
                        <Icon name="chevron_right" rotation={showAttached === 'adapter-details' ? 180 : 0} size="sm" />
                      </Description>
                    </>
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
      {double && (
        <Col $gap="lg" $width="50%">
          <BSCol style={{ width: '100%' }}>
            {showAttached === 'cache-details' && <Cache cache={cache} />}
            {showAttached === 'adapter-details' && <DataAdapter dataAdapter={dataAdapter} />}
          </BSCol>
        </Col>
      )}
    </Row>
  );
}

export default LookupTableView;
