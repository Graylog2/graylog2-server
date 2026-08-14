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

import { Label } from 'components/bootstrap';
import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { OpensearchNode } from './fetchClusterOpensearchNodes';

import CpuMetricsCell from '../shared-components/CpuMetricsCell';
import { SecondaryText } from '../shared-components/NodeMetricsLayout';
import SizeAndRatioMetric from '../shared-components/SizeAndRatioMetric';

const JVM_WARNING_THRESHOLD = 0.95;
const DISK_WARNING_THRESHOLD = 0.7;
const DISK_DANGER_THRESHOLD = 0.8;
const CPU_WARNING_THRESHOLD = 0.7;
const CPU_DANGER_THRESHOLD = 0.9;

export const DEFAULT_VISIBLE_COLUMNS = [
  'name',
  'roles',
  'version',
  'cpu_used_percent',
  'jvm_heap_used_percent',
  'disk_used_percent',
];

const getRoleLabels = (roles: Array<string> | undefined | null) =>
  (roles ?? []).map((role) => (
    <span key={role}>
      <Label bsSize="xs">{role}</Label>&nbsp;
    </span>
  ));

const computeJvmHeapUsedBytes = (max: number | null | undefined, usedPercent: number | null | undefined) =>
  max == null || usedPercent == null ? undefined : Math.round(max * (usedPercent / 100));

export const createColumnRenderers = (): ColumnRenderers<OpensearchNode> => ({
  attributes: {
    name: {
      renderCell: (_value, entity) => entity.name ?? 'N/A',
      minWidth: 250,
    },
    version: {
      renderCell: (_value, entity) => (
        <SecondaryText>
          <span>{entity.version ?? 'N/A'}</span>
        </SecondaryText>
      ),
      minWidth: 120,
    },
    roles: {
      renderCell: (_value, entity) => getRoleLabels(entity.roles),
      minWidth: 220,
    },
    cpu_used_percent: {
      renderCell: (_value, entity) => (
        <CpuMetricsCell
          cpuPercent={entity.cpu_used_percent}
          warningThreshold={CPU_WARNING_THRESHOLD}
          dangerThreshold={CPU_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
      textAlign: 'right',
    },
    jvm_heap_used_percent: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={computeJvmHeapUsedBytes(entity.jvm_heap_max, entity.jvm_heap_used_percent)}
          max={entity.jvm_heap_max}
          ratioPercent={entity.jvm_heap_used_percent}
          warningThreshold={JVM_WARNING_THRESHOLD}
        />
      ),
      staticWidth: 130,
      textAlign: 'right',
    },
    disk_used_percent: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.disk_used}
          max={entity.disk_total}
          ratioPercent={entity.disk_used_percent}
          warningThreshold={DISK_WARNING_THRESHOLD}
          dangerThreshold={DISK_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
      textAlign: 'right',
    },
  },
});
