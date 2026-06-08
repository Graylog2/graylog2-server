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
import { ConfirmDialog, Spinner } from 'components/common';
import useCanArchive from 'components/indices/hooks/useCanArchive';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import type { OutdatedIndex } from 'components/indices/hooks/useOutdatedIndices';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { qualifyUrl } from 'util/URLUtils';

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

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

const actionTitle = (action: IndexAction) => {
  switch (action) {
    case 'archive-delete':
      return 'Archive and delete index';
    case 'reindex-system-index':
      return 'Reindex system index';
    default:
      return 'Delete index';
  }
};

const actionConfirmText = (action: IndexAction) => {
  switch (action) {
    case 'archive-delete':
      return 'Archive and delete';
    case 'reindex-system-index':
      return 'Reindex system index';
    default:
      return 'Delete';
  }
};

const actionMessage = ({ action, index }: ConfirmedAction) => {
  switch (action) {
    case 'archive-delete':
      return (
        <p>
          This will create an archive for <strong>{index.index_name}</strong> and delete the index afterwards.
        </p>
      );
    case 'reindex-system-index':
      return (
        <p>
          This will reindex <strong>{index.index_name}</strong> so it can be used with OpenSearch 3.
        </p>
      );
    default:
      return (
        <p>
          This will permanently delete <strong>{index.index_name}</strong>.
        </p>
      );
  }
};

type IndicesGroup = {
  shortLabel: string;
  indexLabel: string;
  indices: Array<OutdatedIndex>;
};

const groupOutdatedIndices = (indices: Array<OutdatedIndex>): Array<IndicesGroup> => [
  {
    shortLabel: 'Graylog',
    indexLabel: 'Graylog index',
    indices: indices.filter((i) => i.managed_index && !i.system_index),
  },
  {
    shortLabel: 'System',
    indexLabel: 'System index',
    indices: indices.filter((i) => i.system_index),
  },
  {
    shortLabel: 'Foreign',
    indexLabel: 'Foreign index',
    indices: indices.filter((i) => !i.managed_index && !i.system_index),
  },
];

const OutdatedIndexActions = ({
  index,
  onAction,
  canArchive,
}: {
  index: OutdatedIndex;
  onAction: (action: ConfirmedAction) => void;
  canArchive: boolean;
}) => {
  if (index.system_index) {
    return (
      <Button bsSize="xs" bsStyle="primary" onClick={() => onAction({ action: 'reindex-system-index', index })}>
        Reindex
      </Button>
    );
  }

  return (
    <ButtonToolbar style={{ justifyContent: 'flex-end' }}>
      {index.managed_index && canArchive && (
        <Button bsSize="xs" bsStyle="warning" onClick={() => onAction({ action: 'archive-delete', index })}>
          Archive and delete
        </Button>
      )}
      <Button bsSize="xs" bsStyle="danger" onClick={() => onAction({ action: 'delete', index })}>
        Delete
      </Button>
    </ButtonToolbar>
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
                    <Label bsStyle="info" bsSize="xs">
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

const OutdatedIndicesTable = () => {
  const { data: outdatedIndices, isError, isLoading, refetch } = useOutdatedIndices();
  const canArchive = useCanArchive();
  const [confirmedAction, setConfirmedAction] = useState<ConfirmedAction | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const indicesGroups = useMemo(() => groupOutdatedIndices(outdatedIndices), [outdatedIndices]);
  const defaultGroupLabel =
    indicesGroups.find((g) => g.indices.length > 0)?.shortLabel ?? indicesGroups[0].shortLabel;
  const [selectedGroupLabel, setSelectedGroupLabel] = useState<string>(defaultGroupLabel);
  const segments = useMemo(
    () => indicesGroups.map((g) => ({ value: g.shortLabel, label: `${g.shortLabel} (${g.indices.length})` })),
    [indicesGroups],
  );
  const selectedGroup = indicesGroups.find((g) => g.shortLabel === selectedGroupLabel) ?? indicesGroups[0];

  const closeConfirmDialog = () => setConfirmedAction(undefined);

  const handleConfirm = async () => {
    if (!confirmedAction) {
      return;
    }

    setIsSubmitting(true);

    try {
      if (confirmedAction.action === 'delete') {
        await IndexerIndices.remove(confirmedAction.index.index_name);
        UserNotification.success(`Index "${confirmedAction.index.index_name}" was deleted.`);
      }

      if (confirmedAction.action === 'archive-delete') {
        await fetch(
          'POST',
          qualifyUrl(
            `/plugins/org.graylog.plugins.archive/cluster/archives/${confirmedAction.index.index_name}?index_action=DELETE`,
          ),
        );
        UserNotification.success(`Archive and delete job for "${confirmedAction.index.index_name}" was started.`);
      }

      if (confirmedAction.action === 'reindex-system-index') {
        await IndexerIndices.reindex(confirmedAction.index.index_name);
        UserNotification.success(`Index "${confirmedAction.index.index_name}" was reindexed.`);
      }

      await refetch();
      closeConfirmDialog();
    } catch (errorThrown) {
      UserNotification.error(
        String(errorThrown),
        `Could not ${actionConfirmText(confirmedAction.action).toLowerCase()}.`,
      );
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
      <SegmentedControl data={segments} value={selectedGroupLabel} onChange={setSelectedGroupLabel} />
      <IndicesGroupTable group={selectedGroup} onAction={setConfirmedAction} canArchive={canArchive} />

      {confirmedAction && (
        <ConfirmDialog
          show
          title={actionTitle(confirmedAction.action)}
          btnConfirmText={actionConfirmText(confirmedAction.action)}
          isAsyncSubmit
          isSubmitting={isSubmitting}
          onCancel={closeConfirmDialog}
          onConfirm={handleConfirm}
          submitLoadingText="Working...">
          {actionMessage(confirmedAction)}
        </ConfirmDialog>
      )}
    </>
  );
};

export default OutdatedIndicesTable;
