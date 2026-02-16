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
import React, { useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Col, Row, SegmentedControl } from 'components/bootstrap';
import { SearchForm } from 'components/common';
import useProductName from 'brand-customization/useProductName';

import GraylogNodesExpandable from './graylog-nodes/GraylogNodesExpandable';
import DataNodesExpandable from './data-nodes/DataNodesExpandable';

const SectionCol = styled(Col)`
  margin-top: 12px;
  padding-left: 0;
  padding-right: 0;
`;

const ActionsCol = styled(Col)`
  margin-top: 12px;
`;

const ControlsWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
`;

type NodeType = 'all' | 'graylog' | 'data';

const ALL_NODES_PAGE_SIZE = 10;
const SINGLE_NODE_TYPE_PAGE_SIZE = 100;
const ALL_NODES_REFETCH_INTERVAL = 10000;
const SINGLE_NODE_TYPE_REFETCH_INTERVAL = 20000;

const ClusterConfigurationNodes = () => {
  const productName = useProductName();
  const [activeNodeType, setActiveNodeType] = useState<NodeType>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const nodeTypeOptions = useMemo<Array<{ label: string; value: NodeType }>>(
    () => [
      { label: 'All Nodes', value: 'all' },
      { label: `${productName} Nodes`, value: 'graylog' },
      { label: 'Data Nodes', value: 'data' },
    ],
    [productName],
  );
  const normalizedSearch = useMemo(() => searchQuery.trim(), [searchQuery]);
  const handleSearch = useCallback((query: string) => setSearchQuery(query), []);
  const handleResetSearch = useCallback(() => setSearchQuery(''), []);

  const showGraylogNodes = activeNodeType === 'all' || activeNodeType === 'graylog';
  const showDataNodes = activeNodeType === 'all' || activeNodeType === 'data';
  const isAllNodesView = activeNodeType === 'all';
  const pageSizeLimit = isAllNodesView ? ALL_NODES_PAGE_SIZE : SINGLE_NODE_TYPE_PAGE_SIZE;
  const refetchInterval = isAllNodesView ? ALL_NODES_REFETCH_INTERVAL : SINGLE_NODE_TYPE_REFETCH_INTERVAL;

  return (
    <Row>
      <ActionsCol md={12}>
        <ControlsWrapper>
          <SearchForm
            query={searchQuery}
            placeholder="Search nodes…"
            wrapperClass=""
            queryWidth={400}
            topMargin={0}
            onSearch={handleSearch}
            onReset={handleResetSearch}
          />
          <SegmentedControl<NodeType>
            value={activeNodeType}
            data={nodeTypeOptions}
            onChange={(value) => setActiveNodeType(value)}
          />
        </ControlsWrapper>
      </ActionsCol>
      {showGraylogNodes && (
        <SectionCol md={12}>
          <GraylogNodesExpandable
            collapsible={activeNodeType === 'all'}
            searchQuery={normalizedSearch}
            pageSizeLimit={pageSizeLimit}
            refetchInterval={refetchInterval}
            onSelectNodeType={activeNodeType === 'all' ? () => setActiveNodeType('graylog') : undefined}
          />
        </SectionCol>
      )}
      {showDataNodes && (
        <SectionCol md={12}>
          <DataNodesExpandable
            collapsible={activeNodeType === 'all'}
            searchQuery={normalizedSearch}
            pageSizeLimit={pageSizeLimit}
            refetchInterval={refetchInterval}
            onSelectNodeType={activeNodeType === 'all' ? () => setActiveNodeType('data') : undefined}
          />
        </SectionCol>
      )}
    </Row>
  );
};

export default ClusterConfigurationNodes;
