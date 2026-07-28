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
import type { ColorVariant } from '@graylog/sawmill';

import { Label } from 'components/bootstrap';
import { Timestamp } from 'components/common';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import type { IncompatibleIndex } from 'components/indices/hooks/useIncompatibleIndices';

import type { IncompatibleIndexRow } from './fetchIncompatibleIndices';
import type { PendingIndexStatus } from './hooks/usePendingIncompatibleIndexActions';

export const DEFAULT_DISPLAYED_COLUMNS = ['index_name', 'category', 'version', 'begin', 'end'];

type Badge = { text: string; style: ColorVariant };

const primaryTypeBadge = (index: IncompatibleIndex): Badge => {
  if (index.system_index) {
    return { text: 'System', style: 'info' };
  }

  return index.managed_index ? { text: 'Graylog', style: 'success' } : { text: 'Foreign', style: 'warning' };
};

const typeBadges = (index: IncompatibleIndex): Array<Badge> => [
  primaryTypeBadge(index),
  ...(index.warm_index ? [{ text: 'Warm', style: 'default' as const }] : []),
];

const isIndexArchived = (
  indexName: string,
  pendingStatus: PendingIndexStatus | undefined,
  archivedIndexNames: ReadonlySet<string>,
) => pendingStatus?.state !== 'archiving' && (archivedIndexNames.has(indexName) || pendingStatus?.state === 'archived');

const statusBadges = (index: IncompatibleIndex, isArchived: boolean): Array<Badge> => [
  ...(index.active_write_index ? [{ text: 'active write index', style: 'default' as const }] : []),
  ...(isArchived ? [{ text: 'archived already', style: 'success' as const }] : []),
];

const renderBadges = (badges: Array<Badge>) =>
  badges.map(({ text, style }) => (
    <React.Fragment key={text}>
      <Label bsStyle={style} bsSize="xs">
        {text}
      </Label>
      &nbsp;
    </React.Fragment>
  ));

const renderRange = (value: unknown) => (value ? <Timestamp dateTime={value as string} /> : <span>&mdash;</span>);

export const createColumnRenderers = (
  pendingIndexStatuses: Map<string, PendingIndexStatus>,
  archivedIndexNames: ReadonlySet<string>,
): ColumnRenderers<IncompatibleIndexRow> => ({
  attributes: {
    index_name: {
      minWidth: 300,
      renderCell: (_value, index) => {
        const isArchived = isIndexArchived(
          index.index_name,
          pendingIndexStatuses.get(index.index_name),
          archivedIndexNames,
        );

        return (
          <span>
            {index.index_name}
            &nbsp;
            {renderBadges(statusBadges(index, isArchived))}
          </span>
        );
      },
    },
    category: {
      staticWidth: 200,
      renderCell: (_value, index) => <span>{renderBadges(typeBadges(index))}</span>,
    },
    version: {
      renderCell: (_value, index) => index.version || 'Unknown',
    },
    begin: { renderCell: renderRange },
    end: { renderCell: renderRange },
  },
});
