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
import styled, { css } from 'styled-components';

import { IndexerIndices } from '@graylog/server-api';

import { Alert, Button, ButtonToolbar, Label, SegmentedControl, Table } from 'components/bootstrap';
import type { StyleProps } from 'components/bootstrap/Button';
import { ConfirmDialog, Spinner } from 'components/common';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import { outdatedIndicesMockOverride } from './mockOutdatedIndices';

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const ActionsToolbar = styled(ButtonToolbar)`
  justify-content: flex-end;
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

type IndexAction = 'delete' | 'archive-delete' | 'reindex-system-index';

type ConfirmedAction = {
  action: IndexAction;
  index: OutdatedIndex;
};

type ActionDefinition = {
  buttonLabel: string;
  buttonStyle: StyleProps;
  confirmTitle: string;
  confirmText: string;
  confirmationBody: (index: OutdatedIndex) => React.ReactNode;
  run: (index: OutdatedIndex) => Promise<unknown>;
  successMessage: (index: OutdatedIndex) => string;
};

type IndexGroupId = 'graylog' | 'system' | 'foreign';

type IndexGroupDefinition = {
  id: IndexGroupId;
  shortLabel: string;
  indexLabel: string;
  matches: (index: OutdatedIndex) => boolean;
};

type IndicesGroup = Omit<IndexGroupDefinition, 'matches'> & {
  indices: Array<OutdatedIndex>;
};

const INDEX_GROUPS: Array<IndexGroupDefinition> = [
  {
    id: 'graylog',
    shortLabel: 'Graylog',
    indexLabel: 'Graylog index',
    matches: (index) => index.managed_index && !index.system_index,
  },
  {
    id: 'system',
    shortLabel: 'System',
    indexLabel: 'System index',
    matches: (index) => index.system_index,
  },
  {
    id: 'foreign',
    shortLabel: 'Foreign',
    indexLabel: 'Foreign index',
    matches: (index) => !index.managed_index && !index.system_index,
  },
];

const DEFAULT_GROUP_ID = INDEX_GROUPS[0].id;

const archiveAndDeleteIndex = (indexName: string) =>
  fetch(
    'POST',
    qualifyUrl(
      `/plugins/org.graylog.plugins.archive/cluster/archives/${encodeURIComponent(indexName)}?index_action=DELETE`,
    ),
  );

const ACTION_DEFINITIONS: Record<IndexAction, ActionDefinition> = {
  delete: {
    buttonLabel: 'Delete',
    buttonStyle: 'danger',
    confirmTitle: 'Delete index',
    confirmText: 'Delete',
    confirmationBody: (index) => (
      <p>
        This will permanently delete <strong>{index.index_name}</strong>.
      </p>
    ),
    run: (index) => IndexerIndices.remove(index.index_name),
    successMessage: (index) => `Index "${index.index_name}" was deleted.`,
  },
  'archive-delete': {
    buttonLabel: 'Archive and delete',
    buttonStyle: 'warning',
    confirmTitle: 'Archive and delete index',
    confirmText: 'Archive and delete',
    confirmationBody: (index) => (
      <p>
        This will create an archive for <strong>{index.index_name}</strong> and delete the index afterwards.
      </p>
    ),
    run: (index) => archiveAndDeleteIndex(index.index_name),
    successMessage: (index) => `Archive and delete job for "${index.index_name}" was started.`,
  },
  'reindex-system-index': {
    buttonLabel: 'Reindex',
    buttonStyle: 'primary',
    confirmTitle: 'Reindex system index',
    confirmText: 'Reindex system index',
    confirmationBody: (index) => (
      <p>
        This will reindex <strong>{index.index_name}</strong> so it can be used with OpenSearch 3.
      </p>
    ),
    run: (index) => IndexerIndices.reindex(index.index_name),
    successMessage: (index) => `Index "${index.index_name}" was reindexed.`,
  },
};

const groupOutdatedIndices = (indices: Array<OutdatedIndex>): Array<IndicesGroup> =>
  INDEX_GROUPS.map(({ id, shortLabel, indexLabel, matches }) => ({
    id,
    shortLabel,
    indexLabel,
    indices: indices.filter(matches),
  }));

const getAvailableActions = (index: OutdatedIndex, canArchive: boolean): Array<IndexAction> => {
  if (index.system_index) {
    return ['reindex-system-index'];
  }

  return index.managed_index && canArchive ? ['archive-delete', 'delete'] : ['delete'];
};

const getFirstGroupWithIndices = (groups: Array<IndicesGroup>) =>
  groups.find((group) => group.indices.length > 0)?.id ?? DEFAULT_GROUP_ID;

const getSelectedGroup = (groups: Array<IndicesGroup>, selectedGroupId: string) =>
  groups.find((group) => group.id === selectedGroupId) ?? groups[0];

const OutdatedIndexActions = ({
  index,
  onAction,
  canArchive,
}: {
  index: OutdatedIndex;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
}) => {
  const actions = getAvailableActions(index, canArchive);

  return (
    <ActionsToolbar>
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
  canArchive,
}: {
  group: IndicesGroup;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
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
            <th aria-label="Actions" />
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
                <OutdatedIndexActions index={index} onAction={onAction} canArchive={canArchive} />
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </ScrollableTableWrapper>
  );
};

const ActionConfirmDialog = ({
  confirmedAction,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  confirmedAction: ConfirmedAction;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) => {
  const actionDefinition = ACTION_DEFINITIONS[confirmedAction.action];

  return (
    <ConfirmDialog
      show
      title={actionDefinition.confirmTitle}
      btnConfirmText={actionDefinition.confirmText}
      isAsyncSubmit
      isSubmitting={isSubmitting}
      onCancel={onCancel}
      onConfirm={onConfirm}
      submitLoadingText="Working...">
      {actionDefinition.confirmationBody(confirmedAction.index)}
    </ConfirmDialog>
  );
};

const OutdatedIndicesTable = () => {
  const { data: outdatedIndices, isError, isLoading, refetch } = useOutdatedIndices({
    mockData: outdatedIndicesMockOverride,
  });
  const canArchive = useCanArchive();
  const isUsingMockData = !!outdatedIndicesMockOverride;
  const [confirmedAction, setConfirmedAction] = useState<ConfirmedAction | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [selectedGroupId, setSelectedGroupId] = useState<string | undefined>();

  const indicesGroups = useMemo(() => groupOutdatedIndices(outdatedIndices), [outdatedIndices]);
  const firstGroupWithIndices = useMemo(() => getFirstGroupWithIndices(indicesGroups), [indicesGroups]);
  const activeGroupId = selectedGroupId ?? firstGroupWithIndices;
  const selectedGroup = getSelectedGroup(indicesGroups, activeGroupId);
  const segments = useMemo(
    () => indicesGroups.map((group) => ({ value: group.id, label: `${group.shortLabel} (${group.indices.length})` })),
    [indicesGroups],
  );

  const closeConfirmDialog = () => setConfirmedAction(undefined);

  const handleConfirm = async () => {
    if (!confirmedAction) {
      return;
    }

    const actionDefinition = ACTION_DEFINITIONS[confirmedAction.action];

    setIsSubmitting(true);

    try {
      if (isUsingMockData) {
        UserNotification.success(`[Mock] ${actionDefinition.successMessage(confirmedAction.index)}`);
      } else {
        await actionDefinition.run(confirmedAction.index);
        UserNotification.success(actionDefinition.successMessage(confirmedAction.index));
        await refetch();
      }

      closeConfirmDialog();
    } catch (errorThrown) {
      UserNotification.error(String(errorThrown), `Could not ${actionDefinition.confirmText.toLowerCase()}.`);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <Spinner text="Loading outdated indices..." />;
  }

  if (isError) {
    return <Alert bsStyle="danger">Could not load outdated indices.</Alert>;
  }

  if (!outdatedIndices.length) {
    return <Alert bsStyle="success">No outdated indices found.</Alert>;
  }

  return (
    <>
      <Heading>Outdated indices</Heading>
      <SegmentedControl
        data={segments}
        value={activeGroupId}
        onChange={setSelectedGroupId}
        color="warning"
        autoContrast
      />
      <IndicesGroupTable group={selectedGroup} onAction={setConfirmedAction} canArchive={canArchive} />

      {confirmedAction && (
        <ActionConfirmDialog
          confirmedAction={confirmedAction}
          isSubmitting={isSubmitting}
          onCancel={closeConfirmDialog}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
};

export default OutdatedIndicesTable;
