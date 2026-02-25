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

import { MetricsColumn, MetricsRow, StyledLabel } from '../../shared-components/NodeMetricsLayout';

const STATUS_MAP: Record<number, { label: string; style: string }> = {
  1: { label: 'PRIMARY', style: 'success' },
  2: { label: 'SECONDARY', style: 'info' },
  7: { label: 'ARBITER', style: 'default' },
  0: { label: 'STARTUP', style: 'warning' },
  3: { label: 'RECOVERING', style: 'warning' },
  5: { label: 'STARTUP2', style: 'warning' },
  6: { label: 'UNKNOWN', style: 'danger' },
  8: { label: 'DOWN', style: 'danger' },
  9: { label: 'ROLLBACK', style: 'warning' },
  10: { label: 'REMOVED', style: 'danger' },
};

type Props = {
  status: number | undefined | null;
  role: string | undefined | null;
};

const MongodbStatusCell = ({ status, role }: Props) => {
  if (status == null) {
    return null;
  }

  const isStandalone = role?.toUpperCase() === 'STANDALONE';
  const statusInfo = isStandalone
    ? { label: 'STANDALONE', style: 'success' }
    : (STATUS_MAP[status] ?? { label: `UNKNOWN (${status})`, style: 'default' });

  return (
    <MetricsColumn>
      <MetricsRow>
        <StyledLabel
          bsStyle={statusInfo.style}
          bsSize="xs"
          title={statusInfo.label}
          aria-label={statusInfo.label}>
          {statusInfo.label}
        </StyledLabel>
      </MetricsRow>
    </MetricsColumn>
  );
};

export default MongodbStatusCell;
