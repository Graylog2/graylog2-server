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
import { useState } from 'react';
import styled, { css } from 'styled-components';

import useParams from 'routing/useParams';
import useDataNodes, { rejoinDataNode, removeDataNode, startDataNode, stopDataNode } from 'components/datanode/hooks/useDataNodes';
import DataNodesPageNavigation from 'components/datanode/DataNodePageNavigation';
import DocsHelper from 'util/DocsHelper';
import { Row, Col, Label, Button } from 'components/bootstrap';
import { ConfirmDialog, DocumentTitle, PageHeader, RelativeTime, Spinner } from 'components/common';
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

const ActionsCol = styled(Col)`
  text-align: right;
`;
const ActionButton = styled(Button)`
  margin-left: 4px;
`;

const DIALOG_TYPES = {
  STOP: 'stop',
  REJOIN: 'rejoin',
  REMOVE: 'remove',
};

const DIALOG_TEXT = {
  [DIALOG_TYPES.REJOIN]: {
    dialogTitle: 'Rejoin Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to rejoin Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.REMOVE]: {
    dialogTitle: 'Remove Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to remove Data Node "${datanode}"?`,
  },
  [DIALOG_TYPES.STOP]: {
    dialogTitle: 'Stop Data Node',
    dialogBody: (datanode: string) => `Are you sure you want to stop Data Node "${datanode}"?`,
  },
};

const DataNodePage = () => {
  const { dataNodeId } = useParams();
  const { data: { elements }, isInitialLoading } = useDataNodes({
    query: '',
    page: 1,
    pageSize: 0,
  } as SearchParams);

  const [dialogType, setDialogType] = useState<string|null>(null);

  if (isInitialLoading) {
    return <Spinner />;
  }

  const datanode = elements.find((node) => node.node_id === dataNodeId);
  const datanodeDisabled = datanode.data_node_status !== 'AVAILABLE';

  const handleClearState = () => {
    setDialogType(null);
  };

  const handleAction = (action) => {
    switch (action) {
      case DIALOG_TYPES.REJOIN:
        setDialogType(DIALOG_TYPES.REJOIN);

        break;
      case DIALOG_TYPES.REMOVE:
        setDialogType(DIALOG_TYPES.REMOVE);

        break;
      case DIALOG_TYPES.STOP:
        setDialogType(DIALOG_TYPES.STOP);

        break;
      default:
        break;
    }
  };

  const handleConfirm = () => {
    switch (dialogType) {
      case DIALOG_TYPES.REJOIN:
        rejoinDataNode(datanode.node_id).then(() => {
          handleClearState();
        });

        break;
      case DIALOG_TYPES.REMOVE:
        removeDataNode(datanode.node_id).then(() => {
          handleClearState();
        });

        break;
      case DIALOG_TYPES.STOP:
        stopDataNode(datanode.node_id).then(() => {
          handleClearState();
        });

        break;
      default:
        break;
    }
  };

  const isDatanodeRunning = datanode.data_node_status === 'AVAILABLE';
  const isDatanodeRemoved = datanode.data_node_status === 'REMOVED';
  const isRemovingDatanode = datanode.data_node_status === 'REMOVING';

  return (
    <DocumentTitle title={`Data Nodes: ${datanode.hostname}`}>
      <DataNodesPageNavigation />
      <PageHeader title={`Data Nodes: ${datanode.hostname}`}
                  documentationLink={{
                    title: 'Data Nodes documentation',
                    path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                  }} />
      <Row className="content">
        <Col xs={12}>
          <Col xs={9}>
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
          <ActionsCol xs={3}>
            {!isDatanodeRunning && <ActionButton onClick={() => startDataNode(datanode.node_id)} bsSize="small">Start</ActionButton>}
            {isDatanodeRunning && <ActionButton onClick={() => handleAction(DIALOG_TYPES.STOP)} bsSize="small">Stop</ActionButton>}
            {isDatanodeRemoved && <ActionButton onClick={() => handleAction(DIALOG_TYPES.REJOIN)} bsSize="small">Rejoin</ActionButton>}
            {(!isDatanodeRemoved || isRemovingDatanode) && <ActionButton onClick={() => handleAction(DIALOG_TYPES.REMOVE)} bsSize="small">Remove</ActionButton>}
          </ActionsCol>
        </Col>
      </Row>
      {!!dialogType && (
        <ConfirmDialog title={DIALOG_TEXT[dialogType].dialogTitle}
                       show
                       onConfirm={handleConfirm}
                       onCancel={handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(datanode.hostname)}
        </ConfirmDialog>
      )}
    </DocumentTitle>
  );
};

export default DataNodePage;
