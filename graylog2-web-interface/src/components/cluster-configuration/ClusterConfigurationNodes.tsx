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
import styled from 'styled-components';

import { Col, Row, SegmentedControl } from 'components/bootstrap';

import GraylogNodesExpandable from './GraylogNodesExpandable';
import DataNodesExpandable from './DataNodesExpandable';

const SectionCol = styled(Col)`
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const ControlsWrapper = styled.div`
  display: flex;
  justify-content: flex-end;
`;

type NodeSegment = 'all' | 'graylog' | 'data';

const SEGMENT_OPTIONS = [
  { label: 'All Nodes', value: 'all' as NodeSegment },
  { label: 'Graylog Nodes', value: 'graylog' as NodeSegment },
  { label: 'Data Nodes', value: 'data' as NodeSegment },
];

const ClusterConfigurationNodes = () => {
  const [activeSegment, setActiveSegment] = useState<NodeSegment>('all');

  const showGraylogNodes = activeSegment === 'all' || activeSegment === 'graylog';
  const showDataNodes = activeSegment === 'all' || activeSegment === 'data';

  return (
    <Row className="content">
      <SectionCol md={12}>
        <ControlsWrapper>
          <SegmentedControl<NodeSegment>
            value={activeSegment}
            data={SEGMENT_OPTIONS}
            onChange={(value) => setActiveSegment(value)}
          />
        </ControlsWrapper>
      </SectionCol>
      {showGraylogNodes && (
        <SectionCol md={12}>
          <GraylogNodesExpandable collapsible={activeSegment === 'all'} />
        </SectionCol>
      )}
      {showDataNodes && (
        <SectionCol md={12}>
          <DataNodesExpandable collapsible={activeSegment === 'all'} />
        </SectionCol>
      )}
    </Row>
  );
};

export default ClusterConfigurationNodes;
