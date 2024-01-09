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
import styled, { css } from 'styled-components';

import useParams from 'routing/useParams';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import DataNodesPageNavigation from 'components/datanode/DataNodePageNavigation';
import DocsHelper from 'util/DocsHelper';
import { Row, Col, Label } from 'components/bootstrap';
import { DocumentTitle, PageHeader, RelativeTime, Spinner } from 'components/common';
import type { SearchParams } from 'stores/PaginationTypes';
import { CertRenewalButton } from 'components/datanode/DataNodeConfiguration/CertificateRenewal';
import Icon from 'components/common/Icon';

const StyledHorizontalDl = styled.dl(({ theme }) => css`
  margin: ${theme.spacings.md} 0;
  
  > dt {
    clear: left;
    float: left;
    margin-bottom: ${theme.spacings.md};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    width: 160px;
  }
  
  > *:not(dt) {
    margin-bottom: ${theme.spacings.md};
    margin-left: 140px;
  }
`);
const StatusLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;
const BooleanIcon = styled(Icon)<{ value: boolean }>(({ theme, value }) => css`
  color: ${value ? theme.colors.variant.success : theme.colors.variant.danger};
`);
const BooleanValue = ({ value }: { value: boolean }) => (
  <><BooleanIcon name={value ? 'check-circle' : 'times-circle'} value={value} /> {value ? 'yes' : 'no'}</>
);

const DataNodePage = () => {
  const { dataNodeId } = useParams();
  const { data: { elements }, isInitialLoading } = useDataNodes({
    query: '',
    page: 1,
    pageSize: 0,
  } as SearchParams);

  if (isInitialLoading) {
    return <Spinner />;
  }

  const datanode = elements.find((node) => node.node_id === dataNodeId);
  const datanodeDisabled = datanode.data_node_status !== 'AVAILABLE';

  return (
    <DocumentTitle title={`Data Nodes: ${datanode.hostname}`}>
      <DataNodesPageNavigation />
      <PageHeader title={`Data Nodes: ${datanode.hostname}`}
                  documentationLink={{
                    title: 'Data Nodes documentation',
                    path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                  }} />
      <Row className="content">
        <Col md={12}>
          <Col md={5}>
            <h2>Details:</h2>
            <StyledHorizontalDl>
              <dt>Hostname:</dt>
              <dd>{datanode.hostname}</dd>
              <dt>Transport address:</dt>
              <dd>{datanode.transport_address || '-'}</dd>
              <dt>Status:</dt>
              <dd>
                <StatusLabel bsStyle={datanodeDisabled ? 'warning' : 'success'}
                             title={datanode.data_node_status}
                             aria-label={datanode.data_node_status}
                             role="button">
                  {datanode.data_node_status || 'N/A'}
                </StatusLabel>
              </dd>
              <dt>Is leader:</dt>
              <dd><BooleanValue value={datanode.is_leader} /></dd>
              <dt>Certificate valid until:</dt>
              <dd><RelativeTime dateTime={datanode.cert_valid_until} /> <CertRenewalButton nodeId={datanode.node_id} status={datanode.status} /></dd>
            </StyledHorizontalDl>
          </Col>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default DataNodePage;
