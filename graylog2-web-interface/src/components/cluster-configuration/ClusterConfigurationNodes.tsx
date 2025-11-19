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
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';

import { Col, Row, SegmentedControl } from 'components/bootstrap';
import { SearchForm } from 'components/common';

import GraylogNodesExpandable from './graylog-nodes/GraylogNodesExpandable';
import DataNodesExpandable from './data-nodes/DataNodesExpandable';

const SectionCol = styled(Col)`
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const ControlsWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
`;

type NodeSegment = 'all' | 'graylog' | 'data';

const SEGMENT_OPTIONS = [
  { label: 'All Nodes', value: 'all' as NodeSegment },
  { label: 'Graylog Nodes', value: 'graylog' as NodeSegment },
  { label: 'Data Nodes', value: 'data' as NodeSegment },
];

const ALL_SEGMENT_PAGE_SIZE = 10;

const ClusterConfigurationNodes = () => {
  const [activeSegment, setActiveSegment] = useState<NodeSegment>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const normalizedSearch = useMemo(() => searchQuery.trim(), [searchQuery]);

  const showGraylogNodes = activeSegment === 'all' || activeSegment === 'graylog';
  const showDataNodes = activeSegment === 'all' || activeSegment === 'data';
  const segmentLimit = activeSegment === 'all' ? ALL_SEGMENT_PAGE_SIZE : undefined;

  return (
    <Row className="content">
      <SectionCol md={12}>
        <ControlsWrapper>
          <SearchForm
            query={searchQuery}
            placeholder="Search nodes…"
            wrapperClass=""
            queryWidth={400}
            topMargin={0}
            onQueryChange={setSearchQuery}
            onReset={() => setSearchQuery('')}
          />
          <SegmentedControl<NodeSegment>
            value={activeSegment}
            data={SEGMENT_OPTIONS}
            onChange={(value) => setActiveSegment(value)}
          />
        </ControlsWrapper>
      </SectionCol>
      {showGraylogNodes && (
        <SectionCol md={12}>
          <GraylogNodesExpandable
            collapsible={activeSegment === 'all'}
            searchQuery={normalizedSearch}
            pageSizeLimit={segmentLimit}
            onSelectSegment={activeSegment === 'all' ? () => setActiveSegment('graylog') : undefined}
          />
        </SectionCol>
      )}
      {showDataNodes && (
        <SectionCol md={12}>
          <DataNodesExpandable
            collapsible={activeSegment === 'all'}
            searchQuery={normalizedSearch}
            pageSizeLimit={segmentLimit}
            onSelectSegment={activeSegment === 'all' ? () => setActiveSegment('data') : undefined}
          />
        </SectionCol>
      )}
    </Row>
  );
};

export default ClusterConfigurationNodes;
