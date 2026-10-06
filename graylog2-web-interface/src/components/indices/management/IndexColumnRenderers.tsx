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
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import NumberUtils from 'util/NumberUtils';

import type { IndexRow } from './fetchIndices';

export const DEFAULT_DISPLAYED_COLUMNS = [
  'index',
  'health',
  'tier',
  'index_set',
  'primary_shards',
  'replicas',
  'docs_count',
  'store_size_bytes',
];

const HEALTH_STYLES: { [health: string]: ColorVariant } = { green: 'success', yellow: 'warning', red: 'danger' };
const TIER_LABELS: { [tier: string]: string } = { hot: 'Hot', warm: 'Warm' };
const TIER_STYLES: { [tier: string]: ColorVariant } = { hot: 'default', warm: 'info' };

const count = (value: number | null) => (value === null || value === undefined ? '' : NumberUtils.formatNumber(value));

const HealthLabel = ({ index }: { index: IndexRow }) =>
  index.status === 'close' ? (
    <Label bsStyle="default">closed</Label>
  ) : (
    <Label bsStyle={HEALTH_STYLES[index.health] ?? 'default'}>{index.health ?? 'unknown'}</Label>
  );

export const createColumnRenderers = (): ColumnRenderers<IndexRow> => ({
  attributes: {
    index: {
      minWidth: 250,
      renderCell: (_value, index) => (
        <span>
          {index.index} {index.is_write_index && <Label bsStyle="info">write index</Label>}
        </span>
      ),
    },
    health: {
      staticWidth: 'matchHeader',
      renderCell: (_value, index) => <HealthLabel index={index} />,
    },
    tier: {
      staticWidth: 'matchHeader',
      renderCell: (_value, index) => (
        <Label bsStyle={TIER_STYLES[index.tier] ?? 'default'}>{TIER_LABELS[index.tier] ?? index.tier ?? 'unknown'}</Label>
      ),
    },
    index_set: {
      renderCell: (_value, index) => index.index_set_title ?? <i>not managed by Graylog</i>,
    },
    write_index: {
      staticWidth: 'matchHeader',
      renderCell: (_value, index) => (index.is_write_index ? 'Yes' : 'No'),
    },
    primary_shards: { staticWidth: 'matchHeader', renderCell: (_value, index) => count(index.primary_shards) },
    replicas: { staticWidth: 'matchHeader', renderCell: (_value, index) => count(index.replicas) },
    docs_count: { staticWidth: 100, renderCell: (_value, index) => count(index.docs_count) },
    store_size_bytes: {
      staticWidth: 100,
      renderCell: (_value, index) =>
        index.store_size_bytes === null || index.store_size_bytes === undefined
          ? ''
          : NumberUtils.formatBytes(index.store_size_bytes),
    },
  },
});
