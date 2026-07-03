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
import styled, { css } from 'styled-components';

import { Alert, Button, ButtonToolbar, Label, Table } from 'components/bootstrap';
import { ProgressBar } from 'components/common';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';

import type { BulkIndexActionCandidate } from './bulkIndexActions';
import type { PendingIndexStatus } from './hooks/usePendingOutdatedIndexActions';
import { ACTION_DEFINITIONS, getAvailableActions } from './outdatedIndexActions';
import type { ConfirmedAction } from './outdatedIndexActions';
import type { IndicesGroup } from './outdatedIndexGroups';

const ActionsToolbar = styled(ButtonToolbar)`
  justify-content: flex-end;
`;

const BulkActionsToolbar = styled(ButtonToolbar)(
  () => css`
    justify-content: flex-end;
    margin: 0;
  `,
);

const ArchiveProgressBar = styled(ProgressBar)`
  display: inline-flex;
  width: 120px;
  margin-bottom: 0;
  vertical-align: middle;
`;

const ScrollableTableWrapper = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.md};

    & > table {
      margin-bottom: 0;
      table-layout: fixed;
    }

    & thead,
    & tbody {
      display: block;
    }

    & thead tr,
    & tbody tr {
      display: table;
      width: 100%;
      table-layout: fixed;
    }

    & tbody {
      max-height: 300px;
      overflow-y: auto;
      scrollbar-gutter: stable;
    }

    & thead {
      scrollbar-gutter: stable;
      overflow-y: hidden;
    }

    & thead th {
      background-color: ${theme.colors.table.head.background};
    }

    & tr > *:nth-child(1) {
      width: 40%;
      text-align: left;
    }

    & tr > *:nth-child(2) {
      width: 30%;
      text-align: left;
    }

    & tr > *:nth-child(3) {
      width: 30%;
      text-align: right;
    }
  `,
);

const OutdatedIndexActions = ({
  index,
  onAction,
  canArchive,
  pendingStatus,
  alreadyArchived,
}: {
  index: OutdatedIndex;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
  pendingStatus: PendingIndexStatus | undefined;
  alreadyArchived: boolean;
}) => {
  if (pendingStatus?.state === 'archiving') {
    // Avoid flashing an empty 0% bar for indices that archive/delete almost instantly — only show the bar
    // once there is real progress to render.
    return pendingStatus.percent > 0 ? (
      <ArchiveProgressBar
        bars={[
          { value: pendingStatus.percent, label: `${pendingStatus.percent}%`, bsStyle: 'warning', animated: true, striped: true },
        ]}
      />
    ) : (
      <Label bsStyle="warning">Archiving...</Label>
    );
  }

  // "Archived" from either source: this session's finished job (localStorage) or the durable archive catalog
  // (archives made in another session / by retention). Both mean the archive exists but the index is still here.
  const isArchived = alreadyArchived || pendingStatus?.state === 'archived';
  // An already-archived index still offers a plain Delete (so the "delete skipped" cleanup can be finished), but
  // never Archive again — getAvailableActions collapses to ['delete'] for a managed index once archived.
  const actions = getAvailableActions(index, canArchive, isArchived);

  return (
    <ActionsToolbar>
      {isArchived && <Label bsStyle="success">Archived, delete skipped</Label>}
      {pendingStatus?.state === 'failed' && (
        <Label bsStyle="danger" title={pendingStatus.message}>
          Archive failed
        </Label>
      )}
      {actions.map((action) => {
        const actionDefinition = ACTION_DEFINITIONS[action];

        return (
          <Button
            key={action}
            bsSize="xs"
            bsStyle={actionDefinition.buttonStyle}
            onClick={() => onAction({ action, index })}>
            {actionDefinition.buttonLabel}
          </Button>
        );
      })}
    </ActionsToolbar>
  );
};

const IndicesGroupTable = ({
  group,
  onAction,
  onBulkAction,
  canArchive,
  pendingIndexStatuses,
  archivedIndexNames,
  bulkActions,
  isBulkActionSubmitting,
}: {
  group: IndicesGroup;
  onAction: (action: ConfirmedAction) => void;
  onBulkAction: (bulkAction: BulkIndexActionCandidate) => void;
  canArchive: boolean;
  pendingIndexStatuses: Map<string, PendingIndexStatus>;
  archivedIndexNames: Set<string>;
  bulkActions: Array<BulkIndexActionCandidate>;
  isBulkActionSubmitting: boolean;
}) => {
  if (group.indices.length === 0) {
    return <Alert bsStyle="info">No outdated {group.shortLabel} indices.</Alert>;
  }

  return (
    <ScrollableTableWrapper>
      <Table condensed hover striped>
        <thead>
          <tr>
            <th>{group.indexLabel}</th>
            <th>OpenSearch version</th>
            <th aria-label="Actions">
              {bulkActions.length > 0 && (
                <BulkActionsToolbar>
                  {bulkActions.map((bulkAction) => (
                    <Button
                      key={bulkAction.action}
                      bsSize="xs"
                      bsStyle={ACTION_DEFINITIONS[bulkAction.action].buttonStyle}
                      disabled={isBulkActionSubmitting}
                      onClick={() => onBulkAction(bulkAction)}>
                      {bulkAction.buttonLabel} ({bulkAction.targetIndices.length})
                    </Button>
                  ))}
                </BulkActionsToolbar>
              )}
            </th>
          </tr>
        </thead>
        <tbody>
          {group.indices.map((index) => (
            <tr key={index.index_name}>
              <td>
                {index.index_name}
                {index.warm_index && (
                  <>
                    &nbsp;
                    <Label bsStyle="gray" bsSize="xs">
                      warm
                    </Label>
                  </>
                )}
              </td>
              <td>{index.version || 'Unknown'}</td>
              <td>
                <OutdatedIndexActions
                  index={index}
                  onAction={onAction}
                  canArchive={canArchive}
                  pendingStatus={pendingIndexStatuses.get(index.index_name)}
                  alreadyArchived={archivedIndexNames.has(index.index_name)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollableTableWrapper>
  );
};

export default IndicesGroupTable;
