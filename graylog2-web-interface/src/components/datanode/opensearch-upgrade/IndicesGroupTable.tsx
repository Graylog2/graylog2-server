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
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

import type { BulkIndexActionCandidate } from './bulkIndexActions';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';
import { CORE_ACTION_DEFINITIONS, getAvailableActions, useIncompatibleIndexActionDefinitions } from './incompatibleIndexActions';
import type { ConfirmedAction } from './incompatibleIndexActions';
import type { IndicesGroup } from './incompatibleIndexGroups';

const indexNameBadges = (index: IncompatibleIndex, isArchived: boolean) =>
  [
    { text: 'warm', style: 'default', show: index.warm_index },
    { text: 'active write index', style: 'default', show: !!index.active_write_index },
    { text: 'archived already', style: 'success', show: isArchived },
  ] as const;

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

const IncompatibleIndexActions = ({
  index,
  onAction,
  canArchive,
  pendingStatus,
  isArchived,
}: {
  index: IncompatibleIndex;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
  pendingStatus: PendingIndexStatus | undefined;
  isArchived: boolean;
}) => {
  const actionDefinitions = useIncompatibleIndexActionDefinitions();

  if (pendingStatus?.state === 'archiving') {
    // No empty 0% bar flash for jobs that finish almost instantly.
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

  const actions = getAvailableActions(index, canArchive, isArchived);

  return (
    <ActionsToolbar>
      {pendingStatus?.state === 'failed' && (
        <Label bsStyle="danger" title={pendingStatus.message}>
          Archive failed
        </Label>
      )}
      {actions.map((action) => {
        const actionDefinition = actionDefinitions[action];

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
  archivedIndexNames: ReadonlySet<string>;
  bulkActions: Array<BulkIndexActionCandidate>;
  isBulkActionSubmitting: boolean;
}) => {
  if (group.indices.length === 0) {
    return <Alert bsStyle="info">No incompatible {group.shortLabel} indices.</Alert>;
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
                      bsStyle={CORE_ACTION_DEFINITIONS[bulkAction.action].buttonStyle}
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
          {group.indices.map((index) => {
            const pendingStatus = pendingIndexStatuses.get(index.index_name);
            const isArchived =
              pendingStatus?.state !== 'archiving' &&
              (archivedIndexNames.has(index.index_name) || pendingStatus?.state === 'archived');

            return (
              <tr key={index.index_name}>
                <td>
                  {index.index_name}
                  {indexNameBadges(index, isArchived)
                    .filter((badge) => badge.show)
                    .map(({ text, style }) => (
                      <React.Fragment key={text}>
                        &nbsp;
                        <Label bsStyle={style} bsSize="xs">
                          {text}
                        </Label>
                      </React.Fragment>
                    ))}
                </td>
                <td>{index.version || 'Unknown'}</td>
                <td>
                  <IncompatibleIndexActions
                    index={index}
                    onAction={onAction}
                    canArchive={canArchive}
                    pendingStatus={pendingStatus}
                    isArchived={isArchived}
                  />
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
    </ScrollableTableWrapper>
  );
};

export default IndicesGroupTable;
