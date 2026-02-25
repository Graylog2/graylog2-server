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

import type { ColumnRenderers, ColumnSchema } from 'components/common/EntityDataTable';
import NumberUtils from 'util/NumberUtils';

import type { MongodbNode } from './fetchClusterMongodbNodes';
import MongodbStatusCell from './cells/MongodbStatusCell';
import ReplicationLagCell from './cells/ReplicationLagCell';
import StorageUsedCell from './cells/StorageUsedCell';

import { RoleLabel, SecondaryText } from '../shared-components/NodeMetricsLayout';

const REPLICATION_LAG_WARNING_THRESHOLD_MS = 1000;
const REPLICATION_LAG_DANGER_THRESHOLD_MS = 30000;
const STORAGE_WARNING_THRESHOLD = 0.7;
const STORAGE_DANGER_THRESHOLD = 0.8;

export const DEFAULT_VISIBLE_COLUMNS = [
  'name',
  'role',
  'version',
  'status',
  'replicationLag',
  'slowQueryCount',
  'storageUsedPercent',
];

export const createColumnDefinitions = (): Array<ColumnSchema> => [];

export const createColumnRenderers = (): ColumnRenderers<MongodbNode> => ({
  attributes: {
    name: {
      renderCell: (_value, entity) => entity.name ?? 'N/A',
      minWidth: 250,
    },
    role: {
      renderCell: (_value, entity) => {
        const role = entity.role?.toUpperCase();

        if (!role) {
          return <SecondaryText><span>N/A</span></SecondaryText>;
        }

        return <RoleLabel bsSize="xs">{role}</RoleLabel>;
      },
      minWidth: 130,
    },
    version: {
      renderCell: (_value, entity) => (
        <SecondaryText><span>{entity.version ?? 'N/A'}</span></SecondaryText>
      ),
      minWidth: 120,
    },
    status: {
      renderCell: (_value, entity) => <MongodbStatusCell status={entity.status} role={entity.role} />,
      staticWidth: 120,
    },
    replicationLag: {
      renderCell: (_value, entity) => (
        <ReplicationLagCell
          replicationLag={entity.replicationLag}
          role={entity.role}
          warningThreshold={REPLICATION_LAG_WARNING_THRESHOLD_MS}
          dangerThreshold={REPLICATION_LAG_DANGER_THRESHOLD_MS}
        />
      ),
      staticWidth: 'matchHeader',
    },
    slowQueryCount: {
      renderCell: (_value, entity) => {
        const count = entity.slowQueryCount;

        if (count == null) {
          return <SecondaryText><span>N/A</span></SecondaryText>;
        }

        return <SecondaryText><span>{NumberUtils.formatNumber(count)}</span></SecondaryText>;
      },
      staticWidth: 'matchHeader',
    },
    storageUsedPercent: {
      renderCell: (_value, entity) => (
        <StorageUsedCell
          storageUsedPercent={entity.storageUsedPercent}
          warningThreshold={STORAGE_WARNING_THRESHOLD}
          dangerThreshold={STORAGE_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 'matchHeader',
    },
  },
});
